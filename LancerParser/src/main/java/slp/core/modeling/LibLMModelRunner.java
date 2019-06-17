package slp.core.modeling;

import slp.core.modeling.liblm.LMContainer;
import slp.core.modeling.mix.LibMixModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.InnerVocabulary;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;
import sun.security.util.Length;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LibLMModelRunner {
    private LMContainer lmContainer;

    public LibLMModelRunner(LMContainer lmContainer) {
        this.lmContainer = lmContainer;
    }

    public List<Pair<String, Double>> toPredictionsWithProbs(Map<String, Pair<Double, Double>> predProbs) {
        return predProbs.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), ModelRunner.toProb(e.getValue())))
                .sorted((p1, p2) -> -Double.compare(p1.right, p2.right))
                .map(p -> Pair.of(p.left, p.right))
                .collect(Collectors.toList());
    }

    public List<String> libModelGreedyPrediction(LibMixModel model, List<String> libNames, List<Double> weights,
                                                 List<String> tokens, int k) {
        List<String> predTokens = new ArrayList<>();
        List<NGramModel> relevantModels = lmContainer.getRelevantModels(libNames);
        assert relevantModels.size() == libNames.size();
        model.setModels(relevantModels);
        List<List<Integer>> tokenIdList = new ArrayList<>();
        for (NGramModel curModel : relevantModels) {
            InnerVocabulary vocabulary = curModel.getVocabulary();
            List<Integer> tokenIds = new ArrayList<>();
            for (String token : tokens) {
                tokenIds.add(vocabulary.toIndex(token));
            }
            tokenIdList.add(tokenIds);
        }

        String unkToken = Vocabulary.UNK;
        int unkId = Vocabulary.toIndex(unkToken);
        for (int i = 0; i < k; i++) {
            for (List<Integer> ids : tokenIdList) {
                ids.add(unkId);// add a placeholder, will be replaced by predId later
            }
            Map<String, Pair<Double, Double>> predProbs = model.predictToken(tokenIdList, weights, tokenIdList.get(0).size() - 1);
            List<Pair<String, Double>> predictions = toPredictionsWithProbs(predProbs);

            if (predictions.size() > 0) {
                double prob = predictions.get(0).right;
                String predToken = predictions.get(0).left;
//                if (prob < 0.4 * (1 - i / k)) {
//                    // a threshold controls the prediction quality.
//                    break;
//                }
                predTokens.add(predToken);
                tokens.set(tokens.size() - 1, predToken);
                for (int j = 0; j < relevantModels.size(); j++) {
                    NGramModel curModel = relevantModels.get(j);
                    List<Integer> ids = tokenIdList.get(j);
                    int curId = curModel.getVocabulary().toIndex(predToken);
                    ids.set(ids.size() - 1, curId);
                }
            } else {
                predTokens.add(unkToken);
            }
        }
        return predTokens;
    }

    public List<String> libModelGreedyPredictionWithBasicModel(LibMixModel model, List<String> libNames, List<Double> weights,
                                                               Model basicModel, List<String> tokens, int k) {
        List<String> predTokens = new ArrayList<>();
        List<NGramModel> relevantModels = lmContainer.getRelevantModels(libNames);

        model.setModels(relevantModels);
        List<List<Integer>> tokenIdList = new ArrayList<>();
        for (NGramModel curModel : relevantModels) {
            InnerVocabulary vocabulary = curModel.getVocabulary();
            List<Integer> tokenIds = new ArrayList<>();
            for (String token : tokens) {
                tokenIds.add(vocabulary.toIndex(token));
            }
            tokenIdList.add(tokenIds);
        }

        // init basicModel
        basicModel.notify(null);
        Vocabulary.setCheckpoint();
        List<Integer> tokenIds = tokens.stream().map(Vocabulary::toIndex).collect(Collectors.toList());
        if (basicModel instanceof NGramCache) {
            for (int i = 0; i < tokens.size(); i++) {
                basicModel.learnToken(tokenIds, i);
            }
        }
        if (basicModel instanceof MixModel) {
            ((MixModel) basicModel).initCacheModels(tokenIds);
        }

        String unkToken = Vocabulary.UNK;
        int unkId = Vocabulary.toIndex(unkToken);
        for (int i = 0; i < k; i++) {
            for (List<Integer> ids : tokenIdList) {
                ids.add(unkId);// add a placeholder, will be replaced by predId later
            }
            tokenIds.add(unkId);
            Map<String, Pair<Double, Double>> predProbs = model.predictToken(tokenIdList, weights, tokenIds.size() - 1);
            Map<Integer, Pair<Double, Double>> basicModelPredProbs = basicModel.predictToken(tokenIds, tokenIds.size() - 1);
            Map<String, Pair<Double, Double>> basicModelPredProbs2 = new HashMap<>();
            for (Map.Entry<Integer, Pair<Double, Double>> entry : basicModelPredProbs.entrySet()) {
                basicModelPredProbs2.put(Vocabulary.toWord(entry.getKey()), entry.getValue());
            }
            List<Pair<String, Double>> predictions = mixPredictions(predProbs, basicModelPredProbs2);

            if (predictions.size() > 0) {
                double prob = predictions.get(0).right;
                String predToken = predictions.get(0).left;
//                if (prob < 0.4 * (1 - i / k)) {
//                    // a threshold controls the prediction quality.
//                    break;
//                }
                predTokens.add(predToken);
                tokens.set(tokens.size() - 1, predToken);
                tokenIds.set(tokenIds.size() - 1, Vocabulary.toIndex(predToken));
                for (int j = 0; j < relevantModels.size(); j++) {
                    NGramModel curModel = relevantModels.get(j);
                    List<Integer> ids = tokenIdList.get(j);
                    int curId = curModel.getVocabulary().toIndex(predToken);
                    ids.set(ids.size() - 1, curId);
                }
            } else {
                predTokens.add(unkToken);
            }
        }
        Vocabulary.restoreCheckpoint(); // reset vocab for basicModel
        return predTokens;
    }

    private List<Pair<String, Double>> mixPredictions(Map<String, Pair<Double, Double>> predictions, Map<String, Pair<Double, Double>> basicModelpredictions) {
        double weight1 = 0.5;
        double weight2 = 0.5;
        Map<String, Double> mixedPredictionMap = new HashMap<>();

        for (Map.Entry<String, Pair<Double, Double>> entry : predictions.entrySet()) {
            double prob = 0.0;
            if (mixedPredictionMap.containsKey(entry.getKey())) {
                prob += mixedPredictionMap.get(entry.getKey());
            }
            prob += (entry.getValue().left * entry.getValue().right) * weight1;
            mixedPredictionMap.put(entry.getKey(), prob);
        }

        for (Map.Entry<String, Pair<Double, Double>> entry : basicModelpredictions.entrySet()) {
            double prob = 0.0;
            if (mixedPredictionMap.containsKey(entry.getKey())) {
                prob += mixedPredictionMap.get(entry.getKey());
            }
            prob += (entry.getValue().left * entry.getValue().right) * weight2;
            mixedPredictionMap.put(entry.getKey(), prob);
        }

        return mixedPredictionMap.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .sorted((p1, p2) -> -Double.compare(p1.right, p2.right))
                .map(p -> Pair.of(p.left, p.right))
                .collect(Collectors.toList());


    }
}
