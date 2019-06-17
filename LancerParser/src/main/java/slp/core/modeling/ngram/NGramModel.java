package slp.core.modeling.ngram;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.xml.internal.bind.v2.TODO;
import slp.core.counting.Counter;
import slp.core.counting.trie.TrieCounter;
import slp.core.modeling.AbstractModel;
import slp.core.modeling.ModelRunner;
import slp.core.sequencing.NGramSequencer;
import slp.core.translating.InnerVocabulary;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

public abstract class NGramModel extends AbstractModel {

    protected Counter counter;
    protected InnerVocabulary vocabulary = new InnerVocabulary();

    public NGramModel() {
        this(new TrieCounter());
    }

    public NGramModel(Counter counter) {
        this.counter = counter;
    }

    public Counter getCounter() {
        return this.counter;
    }

    @Override
    public void notify(File next) {
    }

    @Override
    public void learn(List<Integer> input) {
        this.counter.countBatch(NGramSequencer.sequenceForward(input));
    }

    @Override
    public void learnToken(List<Integer> input, int index) {
        /*
         * Learning/forgetting a token in the middle of a sequence is not straightforward.
         * We do so by extracting every ngram that the word at index will be part of
         * and learning/forgetting each, while forgetting/learning the context only
         * up to but not including the word's position again (due to aggressive sequence counting)
         */
        List<List<Integer>> sequences = NGramSequencer.sequenceAround(input, index);
        int posInSequence = Math.min(index, ModelRunner.getNGramOrder() - 1);
        for (int i = 0; i < sequences.size(); i++) {
            List<Integer> sequence = sequences.get(i);
            // We must forget context up to the word to prevent double-counting with aggressive sequence counting.
            this.counter.count(sequence);
            if (posInSequence >= 0)
                this.counter.unCount(sequence.subList(0, posInSequence--));
        }
    }

    @Override
    public void forget(List<Integer> input) {
        this.counter.unCountBatch(NGramSequencer.sequenceForward(input));
    }

    @Override
    public void forgetToken(List<Integer> input, int index) {
        /*
         * Learning/forgetting a token in the middle of a sequence is not straightforward.
         * We do so by extracting every ngram that the word at index will be part of
         * and learning/forgetting each, while forgetting/learning the corresponding context
         * up to but not including the word again (due to aggressive sequence counting)
         */
        List<List<Integer>> sequences = NGramSequencer.sequenceAround(input, index);
        int posInSequence = Math.min(index, ModelRunner.getNGramOrder() - 1);
        for (int i = 0; i < sequences.size(); i++) {
            List<Integer> sequence = sequences.get(i);
            // We must learn context up to the word to prevent double-counting with aggressive sequence counting.
            if (posInSequence >= 0) this.counter.count(sequence.subList(0, posInSequence--));
            this.counter.unCount(sequence);
        }
    }

    @Override
    public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
        List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
        double probability = 0.0;
        double mass = 0.0;
        int hits = 0;
        for (int i = sequence.size() - 1; i >= 0; i--) {
            List<Integer> sub = sequence.subList(i, sequence.size());
            // counts[0] = count(with the given input[:-1], the following token == input[-1])
            // counts[1]= count(with the given input[:-1], cnt of all the appeared tokens)
            long[] counts = this.counter.getCounts(sub);
            if (counts[1] == 0) break;
            Pair<Double, Double> resN = this.modelWithConfidence(sub, counts);
            double prob = resN.left;
            double conf = resN.right;    // conf ≈ weight
            mass = (1 - conf) * mass + conf;
            probability = (1 - conf) * probability + conf * prob;
            hits++;
        }
        probability /= mass;
        // In the new model, final confidence is same for all n-gram models, proportional to longest context seen
        double confidence = (1 - Math.pow(2, -hits));
        return Pair.of(probability, confidence);
    }

    protected abstract Pair<Double, Double> modelWithConfidence(List<Integer> subList, long[] counts);

    @Override
    public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
        List<Integer> sequence = NGramSequencer.sequenceAt(input, index - 1);
        Set<Integer> predictions = new HashSet<>();
        int limit = ModelRunner.getPredictionCutoff();
        for (int i = 0; i < sequence.size(); i++) {
            predictions.addAll(this.predictWithConfidence(sequence.subList(i, sequence.size()), limit, predictions));
        }
        return predictions.stream().collect(Collectors.toMap(p -> p, p -> prob(input, index, p)));
    }

    private Map<String, Pair<Integer, List<Integer>>> mem = new HashMap<>();

    protected final List<Integer> predictWithConfidence(List<Integer> indices, int limit, Set<Integer> covered) {
        StringBuilder indicesStringBuilder = new StringBuilder();
        for (int id : indices) {
            indicesStringBuilder.append(id);
        }
        String indicesString = indicesStringBuilder.toString();

        List<Integer> top;
        int key = 31 * (this.counter.getSuccessorCount() + 31 * this.counter.getCount());
        if (this.mem.containsKey(indicesString) && this.mem.get(indicesString).left.equals(key)) {
            top = this.mem.get(indicesString).right;
        } else {
            if (this.mem.containsKey(indicesString)) this.mem.clear();
            top = this.counter.getTopSuccessors(indices, limit);
            if (this.counter.getSuccessorCount(indices) > 1000) {
                this.mem.put(indicesString, Pair.of(key, top));
            }
        }
        return top;
    }

    private Pair<Double, Double> prob(List<Integer> input, int index, int prediction) {
        Integer prev = input.set(index, prediction);
        Pair<Double, Double> prob = this.modelAtIndex(input, index);
        input.set(index, prev);
        return prob;
    }

    private static Class<? extends NGramModel> standard = JMModel.class;

    public static void setStandard(Class<? extends NGramModel> clazz) {
        standard = clazz;
    }

    public static NGramModel standard() {
        try {
            return standard.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
            return new JMModel();
        }
    }

    public static NGramModel standard(Counter counter) {
        try {
            return standard.getConstructor(Counter.class).newInstance(counter);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return new JMModel();
        }
    }

    public InnerVocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(InnerVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public String toString() {
        return "NGramModel{}";
    }
}
