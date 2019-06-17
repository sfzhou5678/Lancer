package slp.core.modeling.mix;

import slp.core.modeling.Model;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.InnerVocabulary;
import slp.core.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibMixModel {
    private List<NGramModel> models = new ArrayList<>();

    public List<NGramModel> getModels() {
        return models;
    }

    public void setModels(List<NGramModel> models) {
        this.models = models;
    }

    public Map<String, Pair<Double, Double>> predictToken(List<List<Integer>> tokenIdList, List<Double> weights, int index) {
        assert tokenIdList.size() == models.size();

        Map<String, List<Pair<Double, Double>>> tokenProbPool = new HashMap<>();
        for (int i = 0; i < models.size(); i++) {
            double weight = weights.get(i);
            NGramModel model = models.get(i);
            List<Integer> input = tokenIdList.get(i);

            Map<Integer, Pair<Double, Double>> idProbMap = model.predictToken(input, index);
            InnerVocabulary vocabulary = model.getVocabulary();
            Map<String, Pair<Double, Double>> tokenProbMap = new HashMap<>();
            for (Map.Entry<Integer, Pair<Double, Double>> entry : idProbMap.entrySet()) {
                String token = vocabulary.toWord(entry.getKey());
                tokenProbMap.put(token, new Pair<>(entry.getValue().left * weight, entry.getValue().right * weight));
            }

            for (Map.Entry<String, Pair<Double, Double>> entry : tokenProbMap.entrySet()) {
                List<Pair<Double, Double>> probList = tokenProbPool.get(entry.getKey());
                if (probList == null) probList = new ArrayList<>();
                probList.add(entry.getValue());
                tokenProbPool.put(entry.getKey(), probList);
            }
        }
        Map<String, Pair<Double, Double>> predTokens = new HashMap<>();
        for (Map.Entry<String, List<Pair<Double, Double>>> entry : tokenProbPool.entrySet()) {
            List<Pair<Double, Double>> probList = tokenProbPool.get(entry.getKey());
            Pair<Double, Double> prob = mix(probList);
            predTokens.put(entry.getKey(), prob);
        }
        return predTokens;
    }

    private Pair<Double, Double> mix(List<Pair<Double, Double>> probList) {
        if (probList == null || probList.size() == 0) {
            return Pair.of(0.0, 0.0);
        }
        double prob = 0.0;
        double conf = 0.0;
        for (Pair<Double, Double> pair : probList) {
            prob += pair.left;
            conf += pair.right;
        }
        prob /= probList.size();
        conf /= probList.size();
        return Pair.of(prob, conf);
    }


}
