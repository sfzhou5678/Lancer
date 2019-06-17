package handler;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import config.SettingConfig;
import gui.LancerMainToolWindow;
import http.LancerHttpClient;
import javafx.util.Pair;
import org.apache.lucene.search.Query;
import retriever.LuceneMultiFiledsQueryBuilder;
import retriever.LuceneRetriever;
import slp.core.infos.MethodInfo;
import slp.core.lexing.code.JavaDetailLexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static slp.core.lexing.DetailLexerRunner.extractCurrentMethodInfo;

public class RecommendSnippetHandler {
    private JavaDetailLexer lexer = new JavaDetailLexer();
    private LuceneRetriever retriever = LuceneRetriever.getRetriever();
    private LancerHttpClient httpClient = new LancerHttpClient("localhost", 58362);

    {
        lexer.setMinSnippetLength(1);
    }

    public void execute(LancerMainToolWindow toolWindow, Editor editor, Document doc) {
        SettingConfig config = SettingConfig.getInstance();
        try {
            int offset = editor.getCaretModel().getOffset();    // the pos (offset) of cursor in the given document.
            String codeContext = doc.getText().substring(0, offset);

            HashMap<String, Object> tokenizeResults = lexer.tokenizeLines(codeContext, false);
            List<String> codeContextTokens= (List<String>) tokenizeResults.get("tokens");
            MethodInfo currentMethod = extractCurrentMethodInfo(lexer,tokenizeResults, codeContext);
            if (currentMethod != null) {
                List<Pair<MethodInfo, Double>> methodInfoList = new ArrayList<>();
                if (config.isENABLE_LOCAL_MODE()) {
                    // 2. local LM & local retriever
                    Query query = LuceneMultiFiledsQueryBuilder.buildQuery(currentMethod);
                    List<MethodInfo> methodInfos = retriever.search(query, 2);
                    for (MethodInfo methodInfo : methodInfos) {
                        methodInfoList.add(new Pair<>(methodInfo, 0.5));
                    }
                }
                if (config.isENABLE_REMORE_MODE()) {
                    // 1. remote LM & remote retriever
                    methodInfoList.addAll(httpClient.searchCode(codeContextTokens, currentMethod, config));
                }

                if (methodInfoList.size() == 0)
                    return;

                // if the retrieved results are different with the current code context, it will be ignored.
                int offset2 = editor.getCaretModel().getOffset();
                if (Math.abs(offset2 - offset) <= 30) {
                    toolWindow.initView();
                    toolWindow.updateView(methodInfoList);
                }
            }
        } catch (Exception exception) {
        }
    }
}
