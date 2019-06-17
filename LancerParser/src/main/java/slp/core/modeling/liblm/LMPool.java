package slp.core.modeling.liblm;


import slp.core.modeling.Model;
import slp.core.modeling.ngram.NGramModel;

import java.io.IOException;

public interface LMPool {
    NGramModel getRelevantModel(String libName, String counterPath, String vocabPath);

    /**
     * 1. When dynamic pool (e.g., LRU Pool) pop some model, this method should be invoked.
     * 2. All the models should be flushed into disk before exit.
     *
     * @param libName
     */
    void saveModel(String libName, String counterPath, String vocabPath) throws IOException;

    void saveAllTheModel() throws IOException;
}
