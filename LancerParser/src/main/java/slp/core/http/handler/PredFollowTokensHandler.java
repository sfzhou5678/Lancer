package slp.core.http.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import slp.core.infos.FileInfo;
import org.apache.commons.io.IOUtils;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.translating.Vocabulary;
import utils.ParserUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PredFollowTokensHandler implements HttpHandler {
    private Model model = null;

    public PredFollowTokensHandler() {
    }

    public PredFollowTokensHandler(Model model) {
        System.out.println("Running PredFollowTokensHandler");
        this.model = model;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestMethod = httpExchange.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("POST")) {
            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, 0); // parse request

            String postQuery = IOUtils.toString(httpExchange.getRequestBody());
            JSONObject exchangeObject = JSON.parseObject(postQuery);
            List<String> tokens = (List<String>) exchangeObject.get("methodTokenSeq");
            int extendLen = exchangeObject.getInteger("extendLen");
            List<String> codeContextTokens = (List<String>) exchangeObject.get("codeContextTokens");

            codeContextTokens = codeContextTokens.stream()
                    .filter(s -> !ParserUtil.LM_STOP_WORDS_SET.contains(s) && !s.isEmpty()).collect(Collectors.toList());


            model.notify(null);
            Vocabulary.setCheckpoint();
            List<Integer> tokenIds = codeContextTokens.stream().map(Vocabulary::toIndex).collect(Collectors.toList());
            List<String> predTokens = ModelRunner.predictFollowingTokens(model, tokenIds, extendLen);
            Vocabulary.restoreCheckpoint();


            String response = String.join(" ", predTokens);
            OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(response.getBytes());
            responseBody.close();
        }
    }
}