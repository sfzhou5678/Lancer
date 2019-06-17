package slp.core.modeling.liblm;

import java.util.ArrayList;
import java.util.List;

public class LibLMInfo {
    private String libName;
    private String counterSavePath;
    private String vocabSavePath;

    private List<String> relevantFilePathList;

    public LibLMInfo(String libName, String counterSavePath, String vocabSavePath) {
        this.libName = libName;
        this.counterSavePath = counterSavePath;
        this.vocabSavePath = vocabSavePath;

        relevantFilePathList = new ArrayList<>();
    }

    public String getLibName() {
        return libName;
    }

    public void setLibName(String libName) {
        this.libName = libName;
    }

    public String getCounterSavePath() {
        return counterSavePath;
    }

    public void setCounterSavePath(String counterSavePath) {
        this.counterSavePath = counterSavePath;
    }

    public String getVocabSavePath() {
        return vocabSavePath;
    }

    public void setVocabSavePath(String vocabSavePath) {
        this.vocabSavePath = vocabSavePath;
    }

    public List<String> getRelevantFilePathList() {
        return relevantFilePathList;
    }

    public void setRelevantFilePathList(List<String> relevantFilePathList) {
        this.relevantFilePathList = relevantFilePathList;
    }
}
