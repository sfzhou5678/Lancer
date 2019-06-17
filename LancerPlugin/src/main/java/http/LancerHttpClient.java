package http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import config.SettingConfig;
import javafx.util.Pair;
import slp.core.infos.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public class LancerHttpClient {
    private String host;
    private int port;

    private String baseUrl;

    public LancerHttpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.baseUrl = String.format("http://%s:%d", host, port);
    }

    public List<Pair<MethodInfo, Double>> searchCode(List<String> codeContextTokens, MethodInfo currentMethod, SettingConfig config) {
        String restfulAPI = "search_codes";
        String url = baseUrl + "/" + restfulAPI;
        JSONObject map = new JSONObject(true);
        map.put("codeContextTokens", codeContextTokens);
        map.put("snippet", currentMethod);
        map.put("useBert", config.isENABLE_DEEP_SEMANTIC());
        String query = map.toJSONString();
        String jsonData = LancerHttpUtil.post(url, query);


        List<Pair<MethodInfo, Double>> results = null;
        if (jsonData != null && !jsonData.isEmpty()) {
            try {
                JSONArray jsonArray = JSON.parseArray(jsonData);
                results = new ArrayList<>();
                for (Object o : jsonArray) {
                    JSONObject jsonObject = (JSONObject) o;
                    MethodInfo methodInfo = JSON.parseObject(JSON.toJSONString(jsonObject.get("methodInfo")), MethodInfo.class);
                    Double score = Double.valueOf(jsonObject.get("score").toString());
                    results.add(new Pair<>(methodInfo, score));
                }
            } catch (Exception e) {
                System.out.println(jsonData);
            }
        }
        return results;
    }
}
