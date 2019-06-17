package utils;

import javax.print.DocFlavor;
import java.util.*;

public class ParserUtil {
    public static final String[] lmStopWords = {
            "abstract", "assert", "boolean", "catch", "char",
            "const", "default", "double", "extends", "final", "finally",
            "short", "static", "strictfp",
            "this", "void",
            "true", "false",
            "(", ")", "{", "}", ";", "@", "-", "+", "*", "/", "[", "]", ",", ":",
            "==", ":", "<", ">", "!=", ">=", "<=", "!"
    };

    public static final Set<String> LM_STOP_WORDS_SET = new HashSet<String>(Arrays.asList(lmStopWords));

    public static final String[] reversedWords = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null",
            "main", "(", ")", "{", "}", ".", ";", "@", "+", "*", "/", "[", "]", "=", ",", "==", ":", "<", ">", "!=", ">=", "<=", "!"
    };

    public static final Set<String> REVERSED_SET = new HashSet<String>(Arrays.asList(reversedWords));

    private static boolean isString(String token) {
        return token.startsWith("\"");
    }

    private static boolean isNum(String token) {
        return token.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$");
    }

    private static boolean isAlpha(String word) {
        return word.matches("[a-zA-Z]+");
    }

    private static String CamelToUnderline(String camelFormat) {
        StringBuilder underlineFormatBuilder = new StringBuilder();
        if (camelFormat.length() > 0)
            underlineFormatBuilder.append(camelFormat.charAt(0));
        for (int i = 1; i < camelFormat.length(); i++) {
            char c = camelFormat.charAt(i);
            if (Character.isUpperCase(c) && !Character.isUpperCase(camelFormat.charAt(i - 1))) {
                underlineFormatBuilder.append("_").append(c);
            } else {
                underlineFormatBuilder.append(c);
            }
        }
        return underlineFormatBuilder.toString().toLowerCase();
    }

    public static List<String> filterReversedWords(List<String> tokens) {
        List<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!REVERSED_SET.contains(token)) {
                filteredTokens.add(token);
            }
        }
        return filteredTokens;
    }

    public static List<String> extractNLwords(List<String> tokens) {
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (token.isEmpty() || isString(token) || isNum(token)) {
                continue;
            }

            if (!REVERSED_SET.contains(token)) {
                String underlineFormatToken = CamelToUnderline(token);

                String[] words = underlineFormatToken.split("_");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    if (isAlpha(word)) {
                        keywords.add(word);
                    }
                }
            }
        }
        return keywords;
    }


}
