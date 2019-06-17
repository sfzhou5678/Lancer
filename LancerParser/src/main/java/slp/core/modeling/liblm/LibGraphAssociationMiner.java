package slp.core.modeling.liblm;

import java.util.*;
import java.util.stream.Stream;

public class LibGraphAssociationMiner extends LibAssociationMiner {
    private double alpha = 0.85;
    private int maxEpoch = 100;

    public LibGraphAssociationMiner(LibAdjacencyMatrix libAdjacencyMatrix, LibAdjacencyMatrix minedWeights) {
        super(libAdjacencyMatrix, minedWeights);
    }

    public LibGraphAssociationMiner(LibAdjacencyMatrix libAdjacencyMatrix) {
        super(libAdjacencyMatrix);
    }


    /**
     * basic version
     */
    @Override
    public void mineAssociation() {
        assert this.libAdjacencyMatrix != null;
        Map<String, Map<String, Double>> matrix = this.libAdjacencyMatrix.matrix;

        for (Map.Entry<String, Map<String, Double>> rootEntry : matrix.entrySet()) {
            String root = rootEntry.getKey();

            Map<String, Double> rank = new HashMap<>();
            for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                rank.put(entry.getKey(), 0.0);
            }
            rank.put(root, 1.0);

            for (int epoch = 0; epoch < maxEpoch; epoch++) {
                Map<String, Double> tempRank = new HashMap<>();
                for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                    tempRank.put(entry.getKey(), 0.0);
                }

                for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                    double val1 = rank.get(entry.getKey());
                    Map<String, Double> relevantLibs = entry.getValue();
                    for (Map.Entry<String, Double> entry2 : relevantLibs.entrySet()) {
                        double val2 = tempRank.get(entry2.getKey());
                        val2 += alpha * val1 / (1.0 * relevantLibs.size());
                        tempRank.put(entry2.getKey(), val2);
                        if (entry2.getKey().equals(root)) {
                            tempRank.put(root, tempRank.get(root) + (1 - alpha));
                        }
                    }
                }
                rank.putAll(tempRank);
            }
            double base = rank.get(root);
            for (Map.Entry<String, Double> entry : rank.entrySet()) {
                if (!entry.getKey().equals(root)) {
                    this.minedWeights.insertEdge(root, entry.getKey(), entry.getValue() / base);
                }
            }
        }
    }

    @Override
    public boolean toFullGraph(String fullGraphSavePath) {
        return super.toFullGraph(fullGraphSavePath);
    }
}
