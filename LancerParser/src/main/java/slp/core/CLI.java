package slp.core;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import db.DBManager;
import slp.core.infos.FileInfo;
import slp.core.idgenerator.BCBIdGenerator;
import slp.core.lexing.code.JavaDetailLexer;
import slp.core.counting.Counter;
import slp.core.counting.giga.GigaCounter;
import slp.core.counting.io.CounterIO;
import slp.core.counting.trie.AbstractTrie;
import slp.core.example.JavaRunner;
import slp.core.example.NLRunner;
import slp.core.http.HTTPServer;
import slp.core.io.Writer;
import slp.core.lexing.DetailLexer;
import slp.core.lexing.DetailLexerRunner;
import slp.core.lexing.Lexer;
import slp.core.lexing.LexerRunner;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.simple.CharacterLexer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.lexing.simple.TokenizedLexer;
import slp.core.lexing.simple.WhitespaceLexer;
import slp.core.modeling.LibLMModelRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.liblm.*;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.LibMixModel;
import slp.core.modeling.mix.NestedModel;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.ngram.ADModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;
import utils.FileUtil;

import static slp.core.modeling.ModelRunner.filterStopWords;

/**
 * Provides a command line interface to a runnable jar produced from this source code.
 * Using SLP_Core as a library can give access to more powerful usage;
 * see {@link JavaRunner} and {@link NLRunner} to get started.
 *
 * @author Vincent Hellendoorn
 */
public class CLI {

    // General options
    private static final String HELP = "(-h|--help)";
    private static final String VERBOSE = "--verbose";

    // Lexing options
    private static final String LANGUAGE = "(-l|--language)";
    private static final String ADD_DELIMS = "--delims";
    private static final String PER_LINE = "(-pl|--per-line)";
    private static final String EXTENSION = "(-e|--extension)";
    private static final String BIG_CLONE_BENCH = "--bcb";


    // Vocabulary options
    private static final String VOCABULARY = "(-v|--vocabulary)";
    private static final String CLOSED = "--closed";
    private static final String UNK_CUTOFF = "(-u|--unk-cutoff)";

    // Training options
    private static final String TRAIN = "(-tr|--train)";
    private static final String ORDER = "(-o|--order)";
    private static final String GIGA = "--giga";

    // Testing options
    private static final String TEST = "(-te|--test)";
    private static final String COUNTER = "--counter";
    private static final String MODEL = "(-m|--model)";
    private static final String SELF = "(-s|--self)";
    private static final String CACHE = "(-c|--cache)";
    private static final String DYNAMIC = "(-d|--dynamic)";
    private static final String NESTED = "(-n|--nested)";

    // LM application options

    private static final String GIVEN_TOKEN_LEN = "-tl";
    private static final String FOLLOWING_NUMS = "-k";
    private static final String FILE_INFO_MAP = "--fileinfo";
    private static final String LIB_CONTAINER = "--lib-container";
    private static final String MINED_WEIGHTS = "--lib-weights";


    private static String[] arguments;
    private static String mode;

    public static FileWriter logger;

    public static void main(String[] args) {
        arguments = args;
        if (arguments.length == 0 || isSet(HELP)) {
            if (arguments.length == 0) System.out.println("No arguments provided, printing help menu.");
            printHelp();
            return;
        }
        setupLexerRunner();
        setupDetailLexerRunner();
        setupVocabulary();
        setupModelRunner();
        setupLogger();
        mode = arguments[0];
        switch (mode.toLowerCase()) {
            case "server": {
                startServer();
                break;
            }
            case "lex": {
                lex();
                break;
            }
            case "lex-detail": {
                lexDetail();
                break;
            }
            case "lex-ix": {
                lex(true);
                break;
            }
            case "vocabulary": {
                buildVocabulary();
                break;
            }
            case "train": {
                train();
                break;
            }
            case "test": {
                test();
                break;
            }
            case "train-test": {
                trainTest();
                break;
            }
            case "predict": {
                predict();
                break;
            }
            case "predict-following": {
                predictFollowingTokens();
                break;
            }
            case "train-predict": {
                trainPredict();
                break;
            }
            case "tokenize": {
                tokenize();
                break;
            }
            case "identify-libnames": {
                identifyLibNames();
                break;
            }
            case "train-liblm": {
                trainLibLM();
                break;
            }
            case "liblm-predict-following": {
                libLMPredictFollowing();
                break;
            }
            default: {
                System.out.println("Command " + mode + " not recognized; use -h for help.");
            }
        }
        teardownLogger();
    }

    private static void identifyLibNames() {
        String trainFolder = getTrain();
        String dataType = trainFolder.substring(trainFolder.lastIndexOf("\\") + 1);
        String libTokensSavePath = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-libTokens.txt";

        File inDir = new File(trainFolder);
        if (!inDir.exists()) {
            System.err.println("Source path for training does not exist: " + inDir);
            return;
        }
        LibIdentifier libIdentifier = new LibIdentifier();
        libIdentifier.learnDir(inDir);
        List<List<String>> libTokens = libIdentifier.identifyLibs();
        System.out.println(libTokens.size());

        FileUtil.writeFileByLines(libTokens.stream().map(l -> String.join("\t", l)).collect(Collectors.toList()),
                libTokensSavePath);
    }

    private static void trainLibLM() {
        String trainFolder = getTrain();
        String dataType = trainFolder.substring(trainFolder.lastIndexOf("\\") + 1);

        File inDir = new File(trainFolder);
        if (!inDir.exists()) {
            System.err.println("Source path for training does not exist: " + inDir);
            return;
        }
        String libTokensSavePath = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-libTokens.txt";

        if (!new File(libTokensSavePath).exists()) {
            System.out.println("Identifying libs...");
            LibIdentifier libIdentifier = new LibIdentifier();
            libIdentifier.learnDir(inDir);
            List<List<String>> libTokens = libIdentifier.identifyLibs();
            System.out.println(String.format("Lib size: %d", libTokens.size()));

            FileUtil.writeFileByLines(libTokens.stream().map(l -> String.join("\t", l)).collect(Collectors.toList()),
                    libTokensSavePath);

            System.out.println("Recording Lib Adjacency Matrix ...");
            String libAdjacencyMatrixSavePath = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-libAdjacencyMatrix.txt";
            String minedLibAssociationSavePath = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-minedLibAssociation.txt";
            LibAdjacencyMatrix matrix;

            LibTrie libNameTrie = new LibTrie();
            libNameTrie.insertLibs(libTokens);

            LibAssiciationRecorder libAssiciationRecorder = new LibAssiciationRecorder(libNameTrie);
            libAssiciationRecorder.recordDir(inDir);
            matrix = libAssiciationRecorder.getLibAdjacencyMatrix();
            matrix.save(libAdjacencyMatrixSavePath);

            System.out.println("Mining Association...");
            LibAssociationMiner miner = new LibGraphAssociationMiner(matrix);
            miner.mineAssociation();
            miner.saveMinedWeights(minedLibAssociationSavePath);
        }
        String containerSavePath = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-LMContainer.container";
        String counterSaveFolder = trainFolder.substring(0, trainFolder.lastIndexOf("\\") + 1) + dataType + "-libLMs";

        File cntSaveFolder = new File(counterSaveFolder);
        if (!cntSaveFolder.exists()) {
            cntSaveFolder.mkdirs();
        }
        LMPool lmPool = new BasicLMPool();
        LMContainer lmContainer = LMContainer.getLMContainer(containerSavePath, counterSaveFolder, libTokensSavePath, lmPool);
        lmContainer.learnDir(inDir);
        try {
            lmContainer.saveModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void libLMPredictFollowing() {
        File inFile = new File(getArg(TEST));
        int givenTokensLength = Integer.valueOf(getArg(GIVEN_TOKEN_LEN));
        int k = Integer.valueOf(getArg(FOLLOWING_NUMS));

        File counterFile = new File(getArg(COUNTER));
        if (!counterFile.exists()) {
            System.err.println("Counter file to read in not found: " + counterFile);
        }
        Model model = getModel();
        ModelRunner.predictFollowingTokensInFile(model, inFile, givenTokensLength, k);

        String containerSavePath = getArg(LIB_CONTAINER);
        LMPool lmPool = new BasicLMPool();
        LMContainer lmContainer = null;
        try {
            lmContainer = LMContainer.restore(containerSavePath, lmPool);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LibLMModelRunner libLMModelRunner = new LibLMModelRunner(lmContainer);
        LibMixModel libMixModel = new LibMixModel();


        Stream<Stream<String>> lineStream = LexerRunner.lex(inFile);
        List<Stream<String>> tmp = lineStream.collect(Collectors.toList());
        List<List<String>> lineTokens = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (Stream<String> s : tmp) lineTokens.add(s.collect(Collectors.toList()));
        for (List<String> t : lineTokens) tokens.addAll(t);

        List<List<String>> libTokens = new ArrayList<>();
        for (List<String> line : lineTokens) {
            if (line.size() > 0 && line.get(0).equals("import") && line.get(line.size() - 1).equals(";")) {
                line = line.subList(1, line.size() - 1);
                libTokens.add(line);
            } else if (libTokens.size() > 0 && line.size() > 2) {
                // simply assume that import only occurs in the front of files.
                break;
            }
        }

        Set<String> invokedLibNameSet = new HashSet<>();
        for (List<String> t : libTokens) {
            List<String> libToken = lmContainer.search(t);
            if (libToken != null) {
                invokedLibNameSet.add(String.join(".", libToken));
            }
        }
        List<String> libNames = new ArrayList<>();
        libNames.addAll(invokedLibNameSet); // Some lib (e.g. java.util) is large, and it need times to load them into mem.
        System.out.println(libNames);

        String minedLibAssociationSavePath = "C:\\Users\\hasee\\Desktop\\bbbbb\\minedLibAssociation.txt";
        LibAdjacencyMatrix libAssiciationMatirx = LibAdjacencyMatrix.load(minedLibAssociationSavePath);
        Map<String, Double> libs = new HashMap<>();
        for (String libName : libNames) {
            libs.put(libName, 1.0);
            Map<String, Double> curLibs = libAssiciationMatirx.getTopK(libName, 5);

            for (Map.Entry<String, Double> entry : curLibs.entrySet()) {
                double weight = entry.getValue();
                if (libs.containsKey(entry.getKey())) {
                    weight += libs.get(entry.getKey());
                }
                weight = Math.min(1.0, weight);
                libs.put(entry.getKey(), weight);
            }
        }

        libNames.clear();
        List<Double> weights = new ArrayList<>();
        for (Map.Entry<String, Double> entry : libs.entrySet()) {
            libNames.add(entry.getKey());
            weights.add(entry.getValue());
        }

        List<String> targetTokens = tokens.subList(givenTokensLength, givenTokensLength + k);
        System.out.println("Target:" + targetTokens.toString());
        tokens = tokens.subList(0, givenTokensLength);
        tokens = filterStopWords(tokens);
//        List<String> predTokens = libLMModelRunner.libModelGreedyPrediction(libMixModel, libNames, tokens, k);
        List<String> predTokens = libLMModelRunner.libModelGreedyPredictionWithBasicModel(libMixModel, libNames, weights, model, tokens, k);
        System.out.println("LibLM:" + predTokens.toString());
    }

    private static void startServer() {
        // default host & port
        String host = "localhost";
        int port = 41235;
        Map<String, Object> extraInfos = new HashMap<>();
        Model model = null;
        try {
            model = getModel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        extraInfos.put("model", model);

        String libContainerSavePath = getArg(LIB_CONTAINER);
        if (libContainerSavePath != null) {
            LMPool lmPool = new BasicLMPool();
            LMContainer lmContainer = null;
            try {
                lmContainer = LMContainer.restore(libContainerSavePath, lmPool);
            } catch (IOException e) {
                e.printStackTrace();
            }

            LibLMModelRunner libLMModelRunner = new LibLMModelRunner(lmContainer);
            LibMixModel libMixModel = new LibMixModel();

            String minedWeightsSavePath = libContainerSavePath.replace("-LMContainer.container", "-minedLibAssociation.txt");
            LibAdjacencyMatrix libAssiciationMatirx = LibAdjacencyMatrix.load(minedWeightsSavePath);

            extraInfos.put("lmContainer", lmContainer);
            extraInfos.put("libLMModelRunner", libLMModelRunner);
            extraInfos.put("libMixModel", libMixModel);
            extraInfos.put("libAssiciationMatirx", libAssiciationMatirx);
        }
        try {
            HTTPServer.serverStart(host, port, extraInfos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, FileInfo> getFileInfoMap() {
        String filePath = getArg(FILE_INFO_MAP);
        Map<String, FileInfo> fileInfoMap = new HashMap<>();
        File fin = new File(filePath);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(fin);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            List<String> jsons = new ArrayList<>();

            int cnt = 0;
//            Gson gson = new Gson();
            JSONObject jsonObject = null;
            FileInfo info = null;
            String line = null;
            while ((line = br.readLine()) != null) {
                jsonObject = JSON.parseObject(line);
                info = JSONObject.toJavaObject(jsonObject, FileInfo.class);

//                info = gson.fromJson(line, FileInfo.class);
                fileInfoMap.put(info.getFileId(), info);

                if (++cnt % 10000 == 0) {
                    System.out.println("Getting file info map, " + cnt);
//                    break;
                }
            }
            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("FileInfoMap: " + fileInfoMap.size());
        return fileInfoMap;
    }

    private static void tokenize() {
    }

    private static void printHelp() {
        System.out.println("\nMain API for command-line usage of SLP-core: can be used to lex corpora, build vocabularies and train/test/predict."
                + "\nAll models are n-gram models at present for the Jar, for more powerful usage, use as library in Java code."
                + "\nEvery use-case starts with lexing, so make sure those options are set correctly.");

        System.out.println("\nUsage:");
        System.out.println("\t-h | --help (or no arguments): Print this help menu)");
        System.out.println("\tlex <in-path> <out-path> [OPTIONS]: lex the in-path (file or directory) to a mirrored structure in out-path."
                + "\n\t\tSee lexing options below, for instance to specify extension filters, delimiter options.");
        System.out.println("\tlex-ix <in-path> <out-path> [OPTIONS]: like lex, excepts translates tokens to integers."
                + "\n\t\tNote: if not given a vocabulary, will build one first and write it to 'train.vocab' in same dir as out-path");
        System.out.println("\tvocabulary <in-path> <out-file> [OPTIONS]: lex the in-path and write resulting vocabulary to out-file.");
        System.out.println("\ttrain --train <path> --counter <out-file> [OPTIONS]: lex all files in in-path, train n-gram model and write to out-file."
                + "\n\t\tCurrently the Jar supports n-gram models only; config-file support may come in further revisions."
                + "\n\t\tNote: if not given a vocabulary, will build one first and write it to 'train.vocab' in same dir as out-file");
        System.out.println("\ttest --test <path> --counter <counts-file> -v <vocab-file> [OPTIONS]: test on files in in-path using counter from counts-file."
                + "\n\t\tNote that the vocabulary parameter is mandatory; a counter is meaningless without a vocabulary."
                + "\n\t\tUse -m (below) to set the model. See also: predict, train-test.");
        System.out.println("\ttrain-test --train <path> --test <path> [OPTIONS]: train on in-path and test modeling accuracy on out-path without storing a counter");
        System.out.println("\tpredict --test <path> --counter <counts-file> [OPTIONS]: test predictions on files in in-path using counter from counts-file."
                + "\n\t\tUse -m (below) to set the model. See also: test, train-predict");
        System.out.println("\ttrain-predict --train <path> --test <path> [OPTIONS]: train on in-path and test prediction accuracy on out-path without storing a counter");

        System.out.println("\nOptions:");
        System.out.println("  General:");
        System.out.println("\t-h | --help: Show this screen");
        System.out.println("\t--verbose <file>: print all output to file");
        System.out.println("  Lexing:");
        System.out.println("\t-l | --language: specify language for tokenization. Currently one of (simple, blank, tokens, java)."
                + "\n\t\t Use 'simple' (default) for splitting on punctuation (preserved as tokens) and whitespace (ignored);"
                + "\n\t\t use 'blank' for just splitting on whitespace and use 'tokens' for pre-tokenized text.");
        System.out.println("\t--delims: explicitly add line delimiters to the start and end of the input. Default: none"
                + "\n\t\tWill add to every line if --per-line is set");
        System.out.println("\t-pl | --per-line: lex and model each line in isolation. Default: false");
        System.out.println("\t-e | --extension: use the provided extension regex to filter input files. Default: none filtered");
        System.out.println("  Vocabulary:");
        System.out.println("\t-v | --vocabulary: specify file to read vocabulary from."
                + "\n\t\tIf none given, vocabulary is constructed 'on the fly' while modeling.");
        System.out.println("\t--closed: close vocabulary after reading, treating every further token as unknown."
                + "\n\t\tNot generally recommended for source code, but sometimes applicable."
                + "\n\t\tHas no effect if vocabulary is built on-line instead of read from file.");
        System.out.println("\t-u | --unk-cutoff: set an unknow token cut-off when building/reading in vocabulary."
                + "\n\t\tAll tokens seen >= cut-off times are preserved. Default: 0, preserving all tokens.");
        System.out.println("  Training:");
        System.out.println("\t-tr | --train: the path to train on");
        System.out.println("\t-o | --order: specify order for n-gram models. Default: 6");
        System.out.println("  Testing:");
        System.out.println("\t-te | --test: the path to test on");
        System.out.println("\t--counter: the path to read the counter from, if testing with pre-trained model");
        System.out.println("\t-m | --model: use specified n-gram smoothing model:"
                + "\n\t\tjm = Jelinek-Mercer, wb = Witten-Bell, ad(m) = (modified) absolute discounting");
        System.out.println("\t-s | --self: specify that we are testing on the train data, implying to 'forget' any data prior to testing.");
        System.out.println("\t-c | --cache: add an n-gram cache model");
        System.out.println("\t-d | --dynamic: dynamically update all models with test data");
        System.out.println("\t-n | --nested: build a nested model of test data (sets dynamic to false); see paper for more details");
        System.out.println();
    }

    private static void setupLexerRunner() {
        LexerRunner.setLexer(getLexer());
        if (isSet(PER_LINE)) LexerRunner.perLine(true);
        if (isSet(ADD_DELIMS)) LexerRunner.addSentenceMarkers(true);
        if (isSet(EXTENSION)) LexerRunner.useExtension(getArg(EXTENSION));
    }

    private static void setupDetailLexerRunner() {
        DetailLexerRunner.setLexer(getDetailLexer());
        if (isSet(PER_LINE)) LexerRunner.perLine(true);
        if (isSet(ADD_DELIMS)) LexerRunner.addSentenceMarkers(true);
        if (isSet(EXTENSION)) LexerRunner.useExtension(getArg(EXTENSION));

    }

    private static void setupVocabulary() {
        if (isSet(VOCABULARY)) {
            String file = getArg(VOCABULARY);
            if (file == null || file.isEmpty() || !new File(file).exists()) return;
            if (isSet(CLOSED)) VocabularyRunner.close(true);
            if (isSet(UNK_CUTOFF)) VocabularyRunner.cutOff(Integer.parseInt(getArg(UNK_CUTOFF)));
            System.out.println("Retrieving vocabulary from file");
            VocabularyRunner.read(new File(file));
        }
        if (isSet(CLOSED)) Vocabulary.close();
    }

    private static void setupModelRunner() {
        if (isSet(PER_LINE)) ModelRunner.perLine(true);
        if (isSelf()) ModelRunner.selfTesting(true);
        if (isSet(ORDER)) ModelRunner.setNGramOrder(Integer.parseInt(getArg(ORDER)));
    }

    private static void setupLogger() {
        if (isSet(VERBOSE)) {
            try {
                logger = new FileWriter(getArg(VERBOSE));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void teardownLogger() {
        if (logger != null) {
            try {
                logger.close();
            } catch (IOException e) {
            }
        }
    }

    private static Lexer getLexer() {
        String language = getArg(LANGUAGE);
        Lexer lexer;
        if (language == null || language.toLowerCase().equals("simple")) lexer = new PunctuationLexer();
        else if (language.toLowerCase().equals("java")) lexer = new JavaLexer();
        else if (language.toLowerCase().equals("tokens")) lexer = new TokenizedLexer();
        else if (language.toLowerCase().equals("blank")) lexer = new WhitespaceLexer();
        else if (language.toLowerCase().equals("char")) lexer = new CharacterLexer();
        else lexer = new PunctuationLexer();
        System.out.println("Using lexer " + lexer.getClass().getSimpleName());
        return lexer;
    }

    private static DetailLexer getDetailLexer() {
        String language = getArg(LANGUAGE);
        DetailLexer lexer;
        if (language.toLowerCase().equals("java")) lexer = new JavaDetailLexer();
        else lexer = null;

        if (isSet(BIG_CLONE_BENCH)) {
            String connURL = "jdbc:postgresql://localhost:5432/bigclonebench";
            String userName = "postgres";
            String password = "postgres";
            DBManager db = new DBManager(connURL, userName, password);
            JavaDetailLexer.idGenerator = new BCBIdGenerator(db);
            System.out.println("Using BCBIdGenerator");
        }
        System.out.println("Using lexer " + lexer.getClass().getSimpleName());
        return lexer;
    }


    private static Counter getCounter() {
//        if (!mode.equals("test") && !mode.equals("predict")) {
//        if (!(mode.equals("test") || mode.startsWith("predict"))) {
//            return isSet(GIGA) ? new GigaCounter() : new JMModel().getCounter();
//        }
        if (mode.startsWith("train")) {
            return isSet(GIGA) ? new GigaCounter() : new JMModel().getCounter();
        } else {
            if (!isSet(COUNTER) || !new File(getArg(COUNTER)).exists()) {
                System.out.println("No (valid) counter file given for test/predict mode! Specify one with --counter *path-to-counter*");
                return null;
            } else {
                long t = System.currentTimeMillis();
                System.out.println("Retrieving counter from file");
                Counter counter = CounterIO.readCounter(new File(getArg(COUNTER)));
                System.out.println("Counter retrieved in " + (System.currentTimeMillis() - t) / 1000 + "s");
                return counter;
            }
        }
    }

    public static Counter getCounter(String counterPath) {
        try {
            File counterFile = new File(counterPath);
            if (counterFile.exists()) {
                Counter counter = CounterIO.readCounter(counterFile);
                return counter;
            } else {
                return new JMModel().getCounter();
            }
        } catch (Exception e) {
            return new JMModel().getCounter();
        }
    }

    public static NGramModel getNGramModel(String counterPath) {
        Counter counter = getCounter(counterPath);

        NGramModel model = new JMModel(counter);
        NGramModel.setStandard(model.getClass());
        AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;

        return model;
    }

    private static Model getModel() {
        Model model = wrapModel(getNGramModel());
        System.out.println("Using model" + model);
        return model;
    }

    public static NGramModel getNGramModel() {
        Counter counter = getCounter();
        String modelName = getArg(MODEL);
        NGramModel model;
        if (modelName == null) model = new JMModel(counter);
        else if (modelName.toLowerCase().equals("jm")) model = new JMModel(counter);
        else if (modelName.toLowerCase().equals("wb")) model = new WBModel(counter);
        else if (modelName.toLowerCase().equals("ad")) model = new ADModel(counter);
        else if (modelName.toLowerCase().equals("adm")) model = new ADMModel(counter);
        else model = new JMModel(counter);
        NGramModel.setStandard(model.getClass());
        if (model instanceof JMModel || model instanceof WBModel) AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;
        return model;
    }


    private static Model wrapModel(Model m) {
        if (isSet(NESTED) && isSet(TEST)) {
            // When loading counter from file, nested self-testing should use 'm' as a local model instead with an empty global model.
            // And since nested models take care of uncounting, we should 'turn off' self-testing now.
            if (isSelf() && !isSet(TRAIN)) {
                ModelRunner.selfTesting(false);
                m = new NestedModel(new File(getArg(TEST)), NGramModel.standard(), m);
            } else {
                m = new NestedModel(new File(getArg(TEST)), m);
            }
        }
        if (isSet(CACHE)) m = new InverseMixModel(m, new NGramCache());
        if (isSet(DYNAMIC)) m.setDynamic(true);
        return m;
    }

    private static void lex() {
        lex(false);
    }

    private static void lex(boolean translate) {
        if (arguments.length >= 3) {
            File inDir = new File(arguments[1]);
            File outDir = new File(arguments[2]);
            LexerRunner.preTranslate(translate);
            boolean emptyVocab = Vocabulary.size() <= 1;
            LexerRunner.lexDirectory(inDir, outDir);
            if (translate && emptyVocab) {
                File vocabFile = isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : new File(outDir.getParentFile(), "train.vocab");
                VocabularyRunner.write(vocabFile);
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Lexing requires at least two arguments: source and target path.");
        }
    }

    private static void lexDetail() {
        boolean translate = false;
        if (arguments.length >= 3) {
            File inDir = new File(arguments[1]);
            File outDir = new File(arguments[2]);
            DetailLexerRunner.preTranslate(translate);
            boolean emptyVocab = Vocabulary.size() <= 1;
            DetailLexerRunner.lexDirectory(inDir, outDir);
            if (translate && emptyVocab) {
                File vocabFile = isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : new File(outDir.getParentFile(), "train.vocab");
                VocabularyRunner.write(vocabFile);
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Lexing requires at least two arguments: source and target path.");
        }
    }

    private static void buildVocabulary() {
        if (arguments.length >= 3) {
            File inDir = new File(arguments[1]);
            File outFile = new File(arguments[2]);
            if (!inDir.exists()) {
                System.err.println("Source path for building vocabulary does not exist: " + inDir);
                return;
            }
            if (isSet(UNK_CUTOFF)) VocabularyRunner.cutOff(Integer.parseInt(getArg(UNK_CUTOFF)));
            VocabularyRunner.build(inDir);
            VocabularyRunner.write(outFile);
        } else {
            System.err.println("Not enough arguments given."
                    + "Building vocabulary requires at least two arguments: source path and output file.");
        }
    }

    private static void train() {
        if (arguments.length >= 5) {
            File inDir = new File(getTrain());
            File outFile = new File(getArg(COUNTER));
            if (!inDir.exists()) {
                System.err.println("Source path for training does not exist: " + inDir);
                return;
            }
            boolean emptyVocab = Vocabulary.size() <= 1;
            NGramModel model = getNGramModel();
            ModelRunner.learn(model, inDir);
            // Since this is for training n-grams only, override ModelRunner's model for easy access to the counter
            Counter counter = model.getCounter();
            // Force GigaCounter.resolve() (if applicable), just for accurate timings below
            counter.getCount();
            long t = System.currentTimeMillis();
            System.out.println("Writing counter to file");
            CounterIO.writeCounter(counter, outFile);
            System.out.println("Counter written in " + (System.currentTimeMillis() - t) / 1000 + "s");
            if (emptyVocab) {
                System.out.println("Writing vocabulary to file");
                File vocabFile = isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : new File(outFile.getParentFile(), "train.vocab");
                VocabularyRunner.write(vocabFile);
                System.out.println("Vocabulary written");
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Training requires at least two arguments: source path and output file.");
        }
    }

    private static void test() {
        if (arguments.length >= 5) {
            File inDir = new File(getTest());
            if (!inDir.exists()) {
                System.err.println("Test path does not exist: " + inDir);
            } else {
                Stream<Pair<File, List<List<Double>>>> fileProbs = ModelRunner.model(getModel(), inDir);
                int[] fileCount = {0};
                DoubleSummaryStatistics stats = ModelRunner.getStats(fileProbs.peek(f -> Writer.writeEntropies(f)).peek(f -> fileCount[0]++));
                System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
                        fileCount[0], stats.getCount(), stats.getAverage());
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Testing requires at least two arguments: test path and counter file.");
        }
    }

    private static void trainTest() {
        if (arguments.length >= 3) {
            File trainDir = new File(getArg(TRAIN));
            File testDir = new File(getArg(TEST));
            if (!trainDir.exists()) {
                System.err.println("Source path for training does not exist: " + trainDir);
                return;
            } else if (!testDir.exists()) {
                System.err.println("Source path for testing does not exist: " + testDir);
                return;
            }
            NGramModel nGramModel = getNGramModel();
            // If self-testing a nested model, simply don't train at all. Do disable 'self' so the ModelRunner won't untrain either.
            if (isSelf() && isSet(NESTED)) ModelRunner.selfTesting(false);
            else ModelRunner.learn(nGramModel, trainDir);
            Model model = wrapModel(nGramModel);
            Stream<Pair<File, List<List<Double>>>> fileProbs = ModelRunner.model(model, testDir);
            int[] fileCount = {0};
            DoubleSummaryStatistics stats = ModelRunner.getStats(fileProbs.peek(f -> Writer.writeEntropies(f)).peek(f -> fileCount[0]++));
            System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
                    fileCount[0], stats.getCount(), stats.getAverage());
        } else {
            System.err.println("Not enough arguments given."
                    + "train-testing requires at least two arguments: train path and test path.");
        }
    }

    private static void predict() {
        if (arguments.length >= 3) {
            File inDir = new File(getArg(TEST));
            File counterFile = new File(getArg(COUNTER));
            if (!inDir.exists()) {
                System.err.println("Test path does not exist: " + inDir);
            } else if (!counterFile.exists()) {
                System.err.println("Counter file to read in not found: " + inDir);
            } else {
                Stream<Pair<File, List<List<Double>>>> fileMRRs = ModelRunner.predict(getModel(), inDir);
                int[] fileCount = {0};
                DoubleSummaryStatistics stats = ModelRunner.getStats(fileMRRs.peek(f -> Writer.writeEntropies(f)).peek(f -> fileCount[0]++));
                System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
                        fileCount[0], stats.getCount(), stats.getAverage());
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Predicting requires two positional arguments: test path and counter file.");
        }
    }

    private static void predictFollowingTokens() {
        if (arguments.length >= 4) {
            File inFile = new File(getArg(TEST));
            File counterFile = new File(getArg(COUNTER));
            int givenTokensLength = Integer.valueOf(getArg(GIVEN_TOKEN_LEN));
            int k = Integer.valueOf(getArg(FOLLOWING_NUMS));
            if (!counterFile.exists()) {
                System.err.println("Counter file to read in not found: " + counterFile);
            } else {
                Model model = getModel();
                ModelRunner.predictFollowingTokensInFile(model, inFile, givenTokensLength, k);
            }
        } else {
            System.err.println("Not enough arguments given."
                    + "Predicting requires two positional arguments: test path and counter file.");
        }
    }

    private static void trainPredict() {
        if (arguments.length >= 3) {
            File trainDir = new File(getArg(TRAIN));
            File testDir = new File(getArg(TEST));
            if (!trainDir.exists()) {
                System.err.println("Source path for training does not exist: " + trainDir);
                return;
            } else if (!testDir.exists()) {
                System.err.println("Source path for testing does not exist: " + testDir);
                return;
            }
            NGramModel nGramModel = getNGramModel();
            // If self-testing a nested model, simply don't train at all. Do disable 'self' so the ModelRunner won't untrain either.
            if (isSelf() && isSet(NESTED)) ModelRunner.selfTesting(false);
            else ModelRunner.learn(nGramModel, trainDir);
            Model model = wrapModel(nGramModel);
            Stream<Pair<File, List<List<Double>>>> fileMRRs = ModelRunner.predict(model, testDir);
            int[] fileCount = {0};
            DoubleSummaryStatistics stats = ModelRunner.getStats(fileMRRs.peek(f -> Writer.writeEntropies(f)).peek(f -> fileCount[0]++));
            System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
                    fileCount[0], stats.getCount(), stats.getAverage());
        } else {
            System.err.println("Not enough arguments given."
                    + "train-predicting requires two positional arguments: train path and test path.");
        }
    }

    private static boolean isSet(String arg) {
        for (String a : arguments) {
            if (a.matches(arg)) return true;
        }
        return false;
    }

    private static String getArg(String arg) {
        for (int i = 1; i < arguments.length; i++) {
            String a = arguments[i];
            if (a.matches(arg)) {
                if (i < arguments.length - 1) return arguments[i + 1];
                return "";
            }
        }
        return null;
    }

    private static String getTrain() {
        return isSet(TRAIN) ? getArg(TRAIN) : "";
    }

    private static String getTest() {
        return isSet(TEST) ? getArg(TEST) : "";
    }

    private static boolean isSelf() {
        // Self testing if SELF has been set, or if TRAIN equal to TEST
        return isSet(SELF) || (isSet(TRAIN) && isSet(TEST) && getArg(TRAIN).equals(getArg(TEST)));
    }
}
