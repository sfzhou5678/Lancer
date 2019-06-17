package slp.core.lexing;

import slp.core.infos.FileInfo;
import slp.core.infos.MethodInfo;
import org.apache.commons.codec.digest.DigestUtils;
import slp.core.io.Reader;
import slp.core.io.Writer;
import slp.core.lexing.code.JavaDetailLexer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.translating.Vocabulary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * The LexerRunner is the starting point of any modeling code,
 * since any input should be lexed first and many models need access to lexing even at test time.
 * This class can be configured statically and exposes static lexing methods that are used by each model.
 *
 * @author Vincent Hellendoorn
 */
public class DetailLexerRunner {

    private static DetailLexer lexer = null;
    private static boolean translate = false;
    private static boolean perLine = false;
    private static boolean sentenceMarkers = false;
    private static String regex = "\\.java";

    /**
     * Specify lexer to be used for tokenizing. Default: {@link PunctuationLexer}.
     *
     * @param lexer The lexer to lex the input with
     */
    public static void setLexer(DetailLexer lexer) {
        DetailLexerRunner.lexer = lexer;
    }

    /**
     * Returns the lexer currently used by this class
     */
    public static DetailLexer getLexer() {
        return DetailLexerRunner.lexer;
    }

    /**
     * Enforce adding delimiters to text to lex (i.e. "&lt;s&gt;", ""&lt;/s&gt;"; see {@link Vocabulary})
     * to each sentence (by default the whole file, unless {@link #perLine(boolean)} is set,
     * in which case each line is treated as a sentence).
     * <br />
     * Default: false, which assumes these have already been added.
     *
     * @return
     */
    public static void addSentenceMarkers(boolean useDelimiters) {
        DetailLexerRunner.sentenceMarkers = useDelimiters;
    }

    /**
     * Returns whether or not file/line (depending on {@code perLine}) sentence markers are added.
     */
    public static boolean addsSentenceMarkers() {
        return DetailLexerRunner.sentenceMarkers;
    }

    /**
     * Enforce lexing each line separately. This only has effect is {@link #useDelimiters()} is set,
     * in which case this method prepends delimiters on each line rather than the full content.
     */
    public static void perLine(boolean perLine) {
        DetailLexerRunner.perLine = perLine;
    }

    /**
     * Returns whether lexing adds delimiters per line.
     */
    public static boolean isPerLine() {
        return DetailLexerRunner.perLine;
    }

    /**
     * Convenience method that translates tokens to indices after lexing before writing to file (default: no translation).
     * <br />
     * <em>Note:</em> you should either initialize the vocabulary yourself or write it to file afterwards
     * (as {@link slp.core.CLI} does) or the resulting indices are (mostly) meaningless.
     */
    public static void preTranslate(boolean preTranslate) {
        DetailLexerRunner.translate = preTranslate;
    }

    /**
     * Specify regex for file extensions to be kept.
     * <br />
     * <em>Note:</em> to just specify the extension, use the more convenient {@link #useExtension(String)}.
     *
     * @param regex Regular expression to match file name against. E.g. ".*\\.(c|h)" for C source and header files.
     */
    public static void useRegex(String regex) {
        DetailLexerRunner.regex = regex;
    }

    /**
     * Alternative to {@link #useRegex(String)} that allows you to specify just the extension.
     * <br />
     * <em>Note:</em> this prepends <code>.*\\.</code> to the provided regex!
     *
     * @param regex Regular expression to match against extension of files. E.g. "(c|h)" for C source and header files.
     */
    public static void useExtension(String regex) {
        DetailLexerRunner.regex = ".*\\." + regex;
    }

    /**
     * Returns the regex currently used to filter input files to lex.
     */
    public static String getRegex() {
        return DetailLexerRunner.regex;
    }

    /**
     * Lex a directory recursively, provided for convenience.
     * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
     *
     * @param from Source file/directory to be lexed
     * @param to   Target file/directory to be created with lexed (optionally translated) content from source
     */
    public static void lexDirectory(File from, File to) {
        int[] count = {0};
        try {
            Files.walk(from.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(fIn -> {
                        if (++count[0] % 1000 == 0) {
                            System.out.println("Lexing at file " + count[0]);
                        }
                        String path = to.getAbsolutePath() + fIn.getAbsolutePath().substring(from.getAbsolutePath().length());
                        File fOut = new File(path);
                        File outDir = fOut.getParentFile();
                        outDir.mkdirs();
                        try {
                            String lexed = lex(fIn, from);
                            Writer.writeContent(fOut, lexed);
                        } catch (IOException e) {
                            System.out.println("Exception in LexerBuilder.tokenize(), from " + fIn + " to " + fOut);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Lex the provided file to a stream of tokens per line.
     * <br />
     * <em>Note:</em> returns empty stream if the file does not match this builder's regex
     * (which accepts everything unless set otherwise in {@link #useRegex(String)}).
     *
     * @param file File to lex
     */
    public static String lex(File file, File from) {
        if (file.getName().endsWith(".java")) {
            FileInfo fileInfo = new FileInfo(file.getName(), from.getAbsolutePath(), file.getAbsolutePath().substring(from.getAbsolutePath().length() + 1));
            String fileId = DigestUtils.md5Hex(fileInfo.getBaseFolderPath() + fileInfo.getRelativeFilePath() + fileInfo.getAffiliatedProject());
            fileInfo.setFileId(fileId);

            return lex(fileInfo, Reader.readLines(file));
        } else
            return "";
    }

    /**
     * Lex the provided lines (see {@link Reader}) to a stream of tokens per line, possibly adding delimiters
     *
     * @param lines Lines to lex
     * @return A Stream of lines containing a Stream of tokens each
     */
    public static String lex(FileInfo fileInfo, Stream<String> lines) {
        String lexedDetail = lexer.lexJson(fileInfo, lines);
        return lexedDetail;
    }

    /**
     * Given lexer and all the written code(codeContext, according to cursor), return the closest (current-edit) method.
     *
     * @param lexer
     * @param codeContext
     * @return
     */
    public static MethodInfo extractCurrentMethodInfo(JavaDetailLexer lexer, HashMap<String, Object> tokenizeResults, String codeContext) {
        FileInfo fileInfo = new FileInfo("", "", "");
        fileInfo.setLineTokens((List<List<String>>) tokenizeResults.get("lineTokens"));

        List<String> lineCodes = Arrays.asList(codeContext.split("\n"));
        List<MethodInfo> methodList = lexer.extractMethodInfos(fileInfo, lineCodes, (List<String>) tokenizeResults.get("tokens"));

        if (methodList != null && methodList.size() > 0) {
            MethodInfo lastMethod = methodList.get(methodList.size() - 1);
            return lastMethod;
        } else {
            return null;
        }
    }
}
