package slp.core.translating;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Translation (to integers) is the second step (after Lexing) before any modeling takes place.
 * The vocabulary is global (static) and is open by default; it can be initialized through
 * the {@link VocabularyRunner} class or simply left open to be filled by the modeling code
 * (as has been shown to be more appropriate for modeling source code).
 * <br />
 * <em>Note:</em> the counts in this class are for informative purposes only:
 * these are not (to be) used by any model nor updated with training.
 *
 * @author Vincent Hellendoorn
 */
public class InnerVocabulary {

    public static final String UNK = "<UNK>";
    public static final String BOS = "<s>";
    public static final String EOS = "</s>";

    private Map<String, Integer> wordIndices;
    private List<String> words;
    private List<Integer> counts;
    private boolean closed = false;
    private int checkPoint;

    public InnerVocabulary() {
        reset();
    }


    private void addUnk() {
        wordIndices.put(UNK, 0);
        words.add(UNK);
        counts.add(0);
    }

    public void reset() {
        wordIndices = new HashMap<>();
        words = new ArrayList<>();
        counts = new ArrayList<>();
        closed = false;
        addUnk();
    }

    public int size() {
        return words.size();
    }

    public void close() {
        closed = true;
    }

    public void open() {
        closed = false;
    }



    public void setCheckpoint() {
        checkPoint = words.size();
    }

    public void restoreCheckpoint() {
        for (int i = words.size(); i > checkPoint; i--) {
            counts.remove(counts.size() - 1);
            String word = words.remove(words.size() - 1);
            wordIndices.remove(word);
        }
    }

    void store(String token, int count) {
        Integer index = wordIndices.get(token);
        if (index == null) {
            index = wordIndices.size();
            wordIndices.put(token, index);
            words.add(token);
            counts.add(count);
        } else {
            counts.set(index, count);
        }
    }


    public Integer toIndex(String token) {
        try {
            token = new String(token.getBytes("iso8859-1"), StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Integer index = wordIndices.get(token);
        if (index == null) {
            if (closed) {
                return wordIndices.get(UNK);
            } else {
                index = wordIndices.size();
                wordIndices.put(token, index);
                words.add(token);
                counts.add(1);
            }
        }
        return index;
    }


    public String toWord(Integer index) {
        return words.get(index);
    }

    public Map<String, Integer> getWordIndices() {
        return wordIndices;
    }

    public void setWordIndices(Map<String, Integer> wordIndices) {
        this.wordIndices = wordIndices;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public List<Integer> getCounts() {
        return counts;
    }

    public void setCounts(List<Integer> counts) {
        this.counts = counts;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public int getCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(int checkPoint) {
        this.checkPoint = checkPoint;
    }
}
