package slp.core.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import slp.core.http.handler.LibLMPredFollowTokensHandler;
import slp.core.infos.FileInfo;
import slp.core.http.handler.PredFollowTokensHandler;
import slp.core.modeling.LibLMModelRunner;
import slp.core.modeling.Model;
import slp.core.modeling.liblm.BasicLMPool;
import slp.core.modeling.liblm.LMContainer;
import slp.core.modeling.liblm.LMPool;
import slp.core.modeling.liblm.LibAdjacencyMatrix;
import slp.core.modeling.mix.LibMixModel;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Map;

public class HTTPServer {
    /**
     * ref: https://blog.csdn.net/xiaoyw71/article/details/79684274
     *
     * @param host
     * @param port
     * @throws IOException
     */
    public static void serverStart(String host, int port) throws IOException {
        HttpServerProvider provider = HttpServerProvider.provider();
        HttpServer httpserver = provider.createHttpServer(new InetSocketAddress(host, port), 100);
        httpserver.createContext("/predTokens", new PredFollowTokensHandler());
        httpserver.setExecutor(null);
        httpserver.start();
        System.out.println("server started");
    }

    public static void serverStart(String host, int port, Map<String, Object> extraInfos) throws IOException {
        HttpServerProvider provider = HttpServerProvider.provider();
        HttpServer httpserver = provider.createHttpServer(new InetSocketAddress(host, port), 100);

        if (extraInfos.containsKey("libMixModel")) {
            httpserver.createContext("/predTokens", new LibLMPredFollowTokensHandler(
                    (LMContainer) extraInfos.get("lmContainer"), (LibLMModelRunner) extraInfos.get("libLMModelRunner"),
                    (LibMixModel) extraInfos.get("libMixModel"), (Model) extraInfos.get("model"),
                    (LibAdjacencyMatrix) extraInfos.get("libAssiciationMatirx")));
        } else {
            httpserver.createContext("/predTokens", new PredFollowTokensHandler((Model) extraInfos.get("model")));
        }

        httpserver.setExecutor(null);
        httpserver.start();
        System.out.println("server started");
    }


    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 41235;
        serverStart(host, port);
    }
}