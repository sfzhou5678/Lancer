package slp.core.modeling.liblm;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import slp.core.CLI;
import slp.core.counting.Counter;
import slp.core.counting.io.CounterIO;
import slp.core.counting.trie.AbstractTrie;
import slp.core.counting.trie.TrieCounter;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.InnerVocabulary;
import slp.core.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BasicLMPool implements LMPool {
    private Map<String, Pair<Pair<String, String>, NGramModel>> lmMap = new HashMap<>();

    public BasicLMPool() {
    }

    public NGramModel getNGramModel(String counterPath, String vocabPath) throws IOException {
        Counter counter = getCounter(counterPath);
        NGramModel model = new JMModel(counter);
        NGramModel.setStandard(model.getClass());
        AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;

        InnerVocabulary vocabulary = getVocabulary(vocabPath);
        model.setVocabulary(vocabulary);

        return model;
    }


    @Override
    public NGramModel getRelevantModel(String libName, String counterPath, String vocabPath) {
        if (lmMap.containsKey(libName)) {
            return lmMap.get(libName).right;
        } else {
            try {
                System.out.println("Loading " + libName);
                NGramModel model = getNGramModel(counterPath, vocabPath);
                lmMap.put(libName, Pair.of(Pair.of(counterPath, vocabPath), model));
                return model;
            } catch (Exception e) {
                System.out.println(libName);
                return null;
            }
        }
    }

    private Counter getCounter(String counterPath) throws IOException {
        File counterFile = new File(counterPath);
        if (counterFile.exists()) {
            Counter counter = CounterIO.readCounter(counterFile);
            return counter;
        } else {
            return new JMModel().getCounter();
        }
    }

    private InnerVocabulary getVocabulary(String vocabPath) throws IOException {
        File file = new File(vocabPath);
        if (file.exists()) {
            String content = FileUtils.readFileToString(file, "UTF-8");
            InnerVocabulary vocabulary = JSON.parseObject(content, InnerVocabulary.class);
            return vocabulary;
        } else {
            return new InnerVocabulary();
        }
    }

    @Override
    public void saveModel(String libName, String counterPath, String vocabPath) throws IOException {
        if (lmMap.containsKey(libName)) {
            Counter counter = lmMap.get(libName).right.getCounter();
            CounterIO.writeCounter(counter, new File(counterPath));
            String content = JSON.toJSONString(lmMap.get(libName).right.getVocabulary());
            FileUtils.writeStringToFile(new File(vocabPath), content);
        }
    }

    @Override
    public void saveAllTheModel() throws IOException {
        for (Map.Entry<String, Pair<Pair<String, String>, NGramModel>> entry : lmMap.entrySet()) {
            saveModel(entry.getKey(), entry.getValue().left.left, entry.getValue().left.right);
        }
    }
}
