package slp.core.http.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import slp.core.modeling.LibLMModelRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.liblm.BasicLMPool;
import slp.core.modeling.liblm.LMContainer;
import slp.core.modeling.liblm.LMPool;
import slp.core.modeling.liblm.LibAdjacencyMatrix;
import slp.core.modeling.mix.LibMixModel;
import slp.core.translating.Vocabulary;
import utils.ParserUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class LibLMPredFollowTokensHandler implements HttpHandler {
    private Model model = null;
    private LMContainer lmContainer;
    private LibLMModelRunner libLMModelRunner;
    private LibMixModel libMixModel;
    private LibAdjacencyMatrix libAssiciationMatirx = null;

    public LibLMPredFollowTokensHandler() {
    }

    public LibLMPredFollowTokensHandler(LMContainer lmContainer, LibLMModelRunner libLMModelRunner,
                                        LibMixModel libMixModel, Model model) {
        System.out.println("Running LibLMPredFollowTokensHandler");
        this.lmContainer = lmContainer;
        this.libLMModelRunner = libLMModelRunner;
        this.libMixModel = libMixModel;
        this.model = model;
    }

    public LibLMPredFollowTokensHandler(LMContainer lmContainer, LibLMModelRunner libLMModelRunner,
                                        LibMixModel libMixModel, Model model, LibAdjacencyMatrix libAssiciationMatirx) {
        System.out.println("Running LibLMPredFollowTokensHandler");
        this.lmContainer = lmContainer;
        this.libLMModelRunner = libLMModelRunner;
        this.libMixModel = libMixModel;
        this.model = model;

        this.libAssiciationMatirx = libAssiciationMatirx;
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
//            List<String> tokens = (List<String>) exchangeObject.get("methodTokenSeq");
            int extendLen = exchangeObject.getInteger("extendLen");
            List<String> codeContextTokens = (List<String>) exchangeObject.get("codeContextTokens");

            // extract lib names before token-filtering
            Set<String> invokedLibNameSet = new HashSet<>();
            int i = 0;
            while (i < codeContextTokens.size()) {
                if (codeContextTokens.get(i).equals("import")) {
                    int j = i + 1;
                    while (j < codeContextTokens.size() && !codeContextTokens.get(j).equals(";")) {
                        j++;
                    }
                    List<String> libToken = lmContainer.search(codeContextTokens.subList(i + 1, j));
                    if (libToken != null) {
                        invokedLibNameSet.add(String.join(".", libToken));
                    }
                    i = j;
                }
                i++;
            }
            List<String> libNames = new ArrayList<>();
            libNames.addAll(invokedLibNameSet);
            List<Double> weights = new ArrayList<>();
            if (libAssiciationMatirx != null) {
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
                for (Map.Entry<String, Double> entry : libs.entrySet()) {
                    libNames.add(entry.getKey());
                    weights.add(entry.getValue());
                }
            } else {
                for (String libName : libNames) {
                    weights.add(1.0);
                }
            }

            codeContextTokens = codeContextTokens.stream()
                    .filter(s -> !ParserUtil.LM_STOP_WORDS_SET.contains(s) && !s.isEmpty()).collect(Collectors.toList());
            List<String> predTokens;
            if (model == null) {
                predTokens = libLMModelRunner.libModelGreedyPrediction(libMixModel, libNames, weights, codeContextTokens, extendLen);
            } else {
                predTokens = libLMModelRunner.libModelGreedyPredictionWithBasicModel(libMixModel, libNames, weights, model, codeContextTokens, extendLen);
            }

            String response = String.join(" ", predTokens);
            OutputStream responseBody = httpExchange.getResponseBody();
            responseBody.write(response.getBytes());
            responseBody.close();
        }
    }
}