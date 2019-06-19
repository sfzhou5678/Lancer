package slp.core.lexing.code;

import com.google.gson.Gson;
import slp.core.idgenerator.IdGenerator;
import slp.core.idgenerator.MD5IdGenerator;
import slp.core.infos.FileInfo;
import slp.core.infos.InfoCollector;
import slp.core.infos.MethodInfo;
import slp.core.lexing.DetailLexer;
import slp.core.lexing.Lexer;

import java.util.*;
import java.util.stream.Collectors;

public class JavaDetailLexer extends JavaLexer implements Lexer, DetailLexer {
    public static IdGenerator idGenerator = new MD5IdGenerator();
    private int minSnippetLength = 6;
    private HashSet<String> keywordReturnTypes = new HashSet<String>() {{
        add("void");
        add("boolean");
        add("short");
        add("int");
        add("long");
        add("float");
        add("double");
        add("byte");
        add("char");
    }};
    private HashSet<String> accessTokens = new HashSet<String>() {{
        add("public");
        add("private");
        add("protected");
        add("default");
    }};
    private HashSet<String> specialMarkTokens = new HashSet<String>() {{
        add("{");   // class
        add("(");   // method
        add("=");   // assign
        add(";");   // define param
    }};

    public String lexJson(FileInfo fileInfo, List<String> lines) {
        String text = String.join("\n",lines);
        HashMap<String, Object> results = tokenizeLines(text, true);
        fileInfo.setLineTokens((List<List<String>>) results.get("lineTokens"));

        List<MethodInfo> methodList = extractMethodInfos(fileInfo, lines, (List<String>) results.get("tokens"));
        if (methodList == null) {
            methodList = new ArrayList<>();
        }
        Gson gson = new Gson();
        InfoCollector infoCollector = new InfoCollector(fileInfo, methodList);
        String jsonDetail = gson.toJson(infoCollector);
        return jsonDetail;
    }


    public List<MethodInfo> extractMethodInfos(FileInfo fileInfo, List<String> lineCodes, List<String> tokens) {
        List<MethodInfo> methodInfoList = new ArrayList<>();
        List<Integer> commentsTokenIndex = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            String t = tokens.get(j);
            if (isComment(t)) {
                commentsTokenIndex.add(j);
            }
        }
        fileInfo.setTokens(tokens);
        fileInfo.setCommentsTokenIndex(commentsTokenIndex);

        // calc the lengths of line tokens to locate the start/end line of method
        List<Integer> cumulativeLineTokenLengths = new ArrayList<>();
        for (List<String> line : fileInfo.getLineTokens()) {
            if (cumulativeLineTokenLengths.size() == 0)
                cumulativeLineTokenLengths.add(line.size());
            else
                cumulativeLineTokenLengths.add(line.size() + cumulativeLineTokenLengths.get(cumulativeLineTokenLengths.size() - 1));
        }
        int curLine = 0;
        String curClassName = "";
        for (int i = 0; i < tokens.size(); ) {
            while (i >= cumulativeLineTokenLengths.get(curLine)) curLine += 1;  // locate cur line
            String token = tokens.get(i);
            if (accessTokens.contains(token)) {
                // find className/methodName
                // care of the attributes
                int startIndex = i;
                int startLine = curLine;
                String accessRight = token;

                List<String> tmpTokens = new ArrayList<>();
                while (!specialMarkTokens.contains(token)) {
                    tmpTokens.add(token);
                    if (i + 1 >= tokens.size()) break;
                    token = tokens.get(++i);
                    while (i >= cumulativeLineTokenLengths.get(curLine)) curLine += 1;  // locate cur line
                }
                List<List<String>> identifiesTokenList;
                switch (token) {
                    case "{": // class
                        identifiesTokenList = findIdentifiers(tmpTokens);
                        if (identifiesTokenList.size() > 0) {
                            curClassName = String.join("", identifiesTokenList.get(0));
                        }
                        break;
                    case "(":
                        String returnType = "";
                        String methodName = "";
                        List<String> paramTypes = new ArrayList<>();
                        List<String> exceptionTypes = new ArrayList<>();
                        String docComments = "";

                        try {
                            identifiesTokenList = findIdentifiers(tmpTokens);
                            if (identifiesTokenList.size() > 1) {
                                returnType = String.join("", identifiesTokenList.get(0));
                                methodName = String.join("", identifiesTokenList.get(1));
                            } else {
                                // have no return Type (void or Constructor)
                                methodName = String.join("", identifiesTokenList.get(0));
                            }
                        } catch (Exception e) {
                            return null;
                        }


                        try {
                            // find the params
                            tmpTokens = new ArrayList<>();
                            token = tokens.get(++i);
                            while (!token.equals(")")) {
                                tmpTokens.add(token);
                                if (i + 1 >= tokens.size()) break;
                                token = tokens.get(++i);
                                while (i >= cumulativeLineTokenLengths.get(curLine)) curLine += 1;  // locate cur line
                            }
                            List<List<String>> paramTypeTokens = findParamTypes(tmpTokens);
                            paramTypes = new ArrayList<>();
                            for (int j = 0; j < paramTypeTokens.size(); ++j) {
                                paramTypes.add(String.join("", paramTypeTokens.get(j)));
                            }
                        } catch (Exception e) {
                            return null;
                        }

                        try {
                            // find the exceptions
                            tmpTokens = new ArrayList<>();
                            token = tokens.get(++i);
                            while (!token.equals("{") && !token.equals(";")) {
                                tmpTokens.add(token);
                                if (i + 1 >= tokens.size()) break;
                                token = tokens.get(++i);
                                while (i >= cumulativeLineTokenLengths.get(curLine)) curLine += 1;  // locate cur line
                            }
                            List<List<String>> exceptionTypeTokens = findExceptionTypes(tmpTokens);
                            exceptionTypes = new ArrayList<>();
                            for (int j = 0; j < exceptionTypeTokens.size(); ++j) {
                                exceptionTypes.add(String.join("", exceptionTypeTokens.get(j)));
                            }
                        } catch (Exception e) {
                            return null;
                        }

                        try {
                            // find the end index
                            int leftParenthesesCount = 0;
                            int rightParenthesesCount = 0;
                            if (token.equals("{")) leftParenthesesCount++;
                            while (leftParenthesesCount != rightParenthesesCount) {
                                if (i + 1 >= tokens.size()) break;
                                token = tokens.get(++i);
                                while (i >= cumulativeLineTokenLengths.get(curLine)) curLine += 1;  // locate cur line

                                if (token.equals("{")) {
                                    leftParenthesesCount += 1;
                                } else if (token.equals("}")) {
                                    rightParenthesesCount += 1;
                                }
                            }
                            int endIndex = i;
                            int endLine = curLine;
                            if (endLine - startLine + 1 < minSnippetLength) {
                                // if this method is too short, break the switch call
                                break;
                            }
                            List<String> methodTokens = tokens.subList(startIndex, endIndex + 1);

                            if (startIndex > 0) {
                                if (tokens.get(startIndex - 1).startsWith("/*")) {
                                    docComments = filterComments(tokens.get(startIndex - 1));
                                }
                            }

                            List<String> inMethodComments = new ArrayList<>();
                            for (int j = 0; j < methodTokens.size(); j++) {
                                String t = methodTokens.get(j);
                                if (isComment(t)) {
                                    t = filterComments(t);
                                    if (!t.isEmpty()) {
                                        inMethodComments.add(t);
                                    }
                                    methodTokens.set(j, "");    // remove comments
                                }
                            }
                            List<String> methodLineCodes = new ArrayList<>();
                            if (lineCodes != null) {
                                methodLineCodes = lineCodes.subList(startLine, endLine + 1);
                            }

                            MethodInfo methodInfo = new MethodInfo(
                                    fileInfo.getFileId(), "", methodName, curClassName,
                                    accessRight, returnType, paramTypes, exceptionTypes,
                                    methodLineCodes, methodTokens,
                                    startIndex, endIndex + 1, startLine, endLine + 1,
                                    docComments, inMethodComments);
                            methodInfo.setMethodId(idGenerator.getId(fileInfo, methodInfo));
                            methodInfoList.add(methodInfo);

                        } catch (Exception e) {
                            return null;
                        }
                        break;
                    default:// param, continue
                        break;
                }
            }
            i++;
        }

//        for (MethodInfo methodInfo : methodInfoList) {
//            System.out.println(methodInfo);
//            System.out.println();
//        }
//        Gson gson = new Gson();
//        String methodListJson = gson.toJson(methodInfoList);
//        return methodListJson;
        return methodInfoList;
    }

    private boolean isComment(String t) {
        return t.isEmpty() || t.startsWith("//") || t.startsWith("/*");
    }

    private String filterComments(String comment) {
        comment = comment.replaceAll("//", "");
        comment = comment.replaceAll("/\\*+", "");
        comment = comment.replaceAll("\\*+/", "");
        comment = comment.replaceAll("\\*", "");
        comment = comment.replaceAll("\\s{2,}", " ");
        comment = comment.trim();
        return comment;
    }

    private List<List<String>> findExceptionTypes(List<String> tmpTokens) {
        List<List<String>> exceptionTypeList = new ArrayList<>();
        List<String> exception = new ArrayList<>();
        for (String t : tmpTokens) {
            if (isComment(t)) continue;
            if (t.equals("throws")) continue;

            if (t.equals(",")) {
                exceptionTypeList.add(exception);
                exception = new ArrayList<>();
            } else {
                exception.add(t);
            }
        }
        if (exception.size() > 0) {
            exceptionTypeList.add(exception);
        }
        return exceptionTypeList;
    }

    private List<List<String>> findParamTypes(List<String> tmpTokens) {
        List<List<String>> paramTypeList = new ArrayList<>();
        List<String> params = new ArrayList<>();
        for (String t : tmpTokens) {
            if (isComment(t)) continue;
            if (t.equals(",")) {
                paramTypeList.add(params.subList(0, params.size() - 1));
                params = new ArrayList<>();
            } else {
                params.add(t);
            }
        }
        if (params.size() > 0) {
            paramTypeList.add(params.subList(0, params.size() - 1));
        }

        return paramTypeList;
    }


    private List<List<String>> findIdentifiers(List<String> tmpTokens) {
        List<List<String>> identifiesTokenList = new ArrayList<>();
        List<String> idTokens = new ArrayList<>();
        for (String t : tmpTokens) {
            if (isComment(t)) continue;
            if (isKeyword(t)) {
                if (idTokens.size() > 0) {
                    identifiesTokenList.add(idTokens);
                    idTokens = new ArrayList<>();
                }
                if (keywordReturnTypes.contains(t)) {
                    idTokens.add(t);
                    identifiesTokenList.add(idTokens);
                    idTokens = new ArrayList<>();
                }
            } else {
                if (idTokens.size() > 0 && !t.equals(".") && !idTokens.get(idTokens.size() - 1).equals(".")) {
                    identifiesTokenList.add(idTokens);
                    idTokens = new ArrayList<>();
                }
                idTokens.add(t);
            }
        }
        if (idTokens.size() > 0) {
            identifiesTokenList.add(idTokens);
        }
        return identifiesTokenList;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public int getMinSnippetLength() {
        return minSnippetLength;
    }

    public void setMinSnippetLength(int minSnippetLength) {
        this.minSnippetLength = minSnippetLength;
    }
}
