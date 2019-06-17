package slp.core.modeling.liblm;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import slp.core.lexing.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.InnerVocabulary;
import slp.core.translating.Vocabulary;
import utils.FileUtil;
import utils.ParserUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container of lib-separated ngram language model.
 * Given lib name, return relevant lm from lmPool or return null if there is no relevant lm.
 */
public class LMContainer {
    private String containerSavePath;
    private String counterSaveFolder;
    private String libTokensSavePath;
    private LibTrie libNameTrie;
    private static LMContainer lmContainer = null;

    private int totalFileCount = 0;
    private Map<String, LibLMInfo> libLMInfoMap = new HashMap<>();
    private transient LMPool lmPool;


    private LMContainer(String containerSavePath, String counterSaveFolder, String libTokensSavePath,
                        LMPool lmPool) {
        this.containerSavePath = containerSavePath;
        this.counterSaveFolder = counterSaveFolder;
        this.libTokensSavePath = libTokensSavePath;

        this.lmPool = lmPool;

//        assert new File(libTokensSavePath).exists(); // libTokensSavePath is built by "identifyLib" function
        List<List<String>> libTokens = FileUtil.readLines(libTokensSavePath).
                stream().map(s -> Arrays.asList(s.split("\t"))).collect(Collectors.toList());
        System.out.println(String.format("Load libTokens from %s", libTokensSavePath));

        this.libNameTrie = new LibTrie(libTokens);
        File saveFolder = new File(counterSaveFolder);
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }
    }

    /**
     * Singleton
     *
     * @return
     */
    public static LMContainer getLMContainer(String containerSavePath, String counterSaveFolder, String libTokensSavePath,
                                             LMPool lmPool) {
        if (lmContainer == null) {
            lmContainer = new LMContainer(containerSavePath, counterSaveFolder, libTokensSavePath, lmPool);
        }
        return lmContainer;
    }

    public List<String> search(List<String> tokens) {
        return libNameTrie.search(tokens);
    }

    /**
     * @param libNames
     * @return
     */
    public List<NGramModel> getRelevantModels(List<String> libNames) {
        List<NGramModel> models = new ArrayList<>();
        for (String libName : libNames) {
            LibLMInfo lmInfo;
            if (libLMInfoMap.containsKey(libName)) {
                lmInfo = libLMInfoMap.get(libName);
                NGramModel model = lmPool.getRelevantModel(libName, lmInfo.getCounterSavePath(), lmInfo.getVocabSavePath());
                if (model != null)
                    models.add(model);
            } else {
                // just continue, since new a model is useless when predicting.
            }
        }
        return models;
    }

    public void learnDir(File dir) {
        long[] learnCounts = new long[]{0, 0};
        long[] learnTime = new long[]{-System.currentTimeMillis()};
        try {
            Files.walk(dir.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .peek(f -> {
                        if (++learnCounts[0] % 1000 == 0) {
                            System.out.println("Counting at file " + learnCounts[0] + " in " + (learnTime[0] + System.currentTimeMillis()) / 1000 + "s");
                        }
                    })
                    .forEach(f -> learnFile(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void learnFile(File file) {
        // 1. parse given file
        Stream<Stream<String>> lineStream = LexerRunner.lex(file);
        List<Stream<String>> tmp = lineStream.collect(Collectors.toList());
        List<List<String>> lineTokens = new ArrayList<>();
        for (Stream<String> s : tmp) {
            lineTokens.add(s.collect(Collectors.toList()));
        }
        this.totalFileCount++;

        // 2. extract imported libs
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
        for (List<String> tokens : libTokens) {
            List<String> libToken = libNameTrie.search(tokens);
            if (libToken != null) {
                invokedLibNameSet.add(String.join(".", libToken));
            }
        }

        List<String> libNames = new ArrayList<>();
        libNames.addAll(invokedLibNameSet);

        // 3. get relevant model for each lib according to libName, append this file in LMInfo, then let these models learn the parsed file
        // filter stop words
        List<String> tokens = new ArrayList<>();
        for (List<String> t : lineTokens) {
            for (String token : t) {
                if (!ParserUtil.LM_STOP_WORDS_SET.contains(token) && !token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }
        for (String libName : libNames) {
            LibLMInfo lmInfo;
            if (libLMInfoMap.containsKey(libName)) {
                lmInfo = libLMInfoMap.get(libName);
            } else {
                String counterSavePath = counterSaveFolder + File.separator + libName + ".counter";
                String vocabSavePath = counterSaveFolder + File.separator + libName + ".vocab";
                lmInfo = new LibLMInfo(libName, counterSavePath, vocabSavePath);
                libLMInfoMap.put(libName, lmInfo);
            }
            NGramModel model = lmPool.getRelevantModel(libName, lmInfo.getCounterSavePath(), lmInfo.getVocabSavePath());
            InnerVocabulary vocabulary = model.getVocabulary();
            List<Integer> ids = new ArrayList<>();
            for (String token : tokens) {
                int id = vocabulary.toIndex(token);
                ids.add(id);
            }
            model.learn(ids);
        }
    }

    public void saveModel() throws IOException {
        // then save LMContainer to disk.
        Gson gson = new Gson();
        String content = gson.toJson(lmContainer);
        FileUtils.writeStringToFile(new File(containerSavePath), content);

        // flush files in pool to disk
        lmPool.saveAllTheModel();
    }

    public static LMContainer restore(String containerSavePath, LMPool lmPool) throws IOException {
        if (lmContainer != null) {
            System.out.println("Restore and overwrite old LMContainer");
        }
        File file = new File(containerSavePath);
        String content = FileUtils.readFileToString(file, "UTF-8");
        Gson gson = new Gson();
        lmContainer = gson.fromJson(content, LMContainer.class);
        lmContainer.lmPool = lmPool;

        return lmContainer;
    }
}
