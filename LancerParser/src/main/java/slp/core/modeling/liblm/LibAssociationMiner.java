package slp.core.modeling.liblm;

import slp.core.util.Pair;
import sun.security.util.Length;

import java.io.*;
import java.util.*;

public class LibAssociationMiner {
    protected LibAdjacencyMatrix libAdjacencyMatrix;
    protected LibAdjacencyMatrix minedWeights = new LibAdjacencyMatrix(true);

    public LibAssociationMiner(LibAdjacencyMatrix libAdjacencyMatrix, LibAdjacencyMatrix minedWeights) {
        this.libAdjacencyMatrix = libAdjacencyMatrix;
        this.minedWeights = minedWeights;
    }

    public LibAssociationMiner(LibAdjacencyMatrix libAdjacencyMatrix) {
        this.libAdjacencyMatrix = libAdjacencyMatrix;
    }

    public void mineAssociation() {
        assert this.libAdjacencyMatrix != null;
        Map<String, Map<String, Double>> matrix = this.libAdjacencyMatrix.matrix;

        for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
            Map<String, Double> relevantLibs = entry.getValue();
            int freqSum = 0;
            for (Map.Entry<String, Double> entry2 : relevantLibs.entrySet()) {
                freqSum += entry2.getValue();
            }
            for (Map.Entry<String, Double> entry2 : relevantLibs.entrySet()) {
                // simple weight - based
                double weight = entry2.getValue() / freqSum;
                this.minedWeights.insertEdge(entry.getKey(), entry2.getKey(), weight);
            }
        }
    }

    public boolean saveMinedWeights(String savePath) {
        try {
            this.minedWeights.save(savePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean toFullGraph(String fullGraphSavePath) {
        assert this.libAdjacencyMatrix != null;
        Map<String, Map<String, Double>> matrix = this.libAdjacencyMatrix.matrix;
        assert this.minedWeights.matrix.size() == matrix.size();
        File file = new File(fullGraphSavePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(fullGraphSavePath);
            BufferedWriter bw = new BufferedWriter(writer);

            Map<String, String> libName2Id = new HashMap<>();
            int libNameIdCnt = 1;
            for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                String libId = String.format("E%06d", libNameIdCnt++);
                libName2Id.put(entry.getKey(), libId);
                bw.write(String.format("%s\t%s\tnull\n", libId, entry.getKey()));
            }

            List<String> relationIds = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                double weight = i / 100.0;
                String relId = String.format("R%06d", i);
                relationIds.add(relId);
                String relName = String.format("%.2f", weight);
                bw.write(String.format("%s\t%s\tnull\n", relId, relName));
            }

            bw.write("\nsplit\n");
            for (Map.Entry<String, Map<String, Double>> entry : minedWeights.matrix.entrySet()) {
                String libId1 = libName2Id.get(entry.getKey());
                Map<String, Double> relevantLibs = entry.getValue();

                Map<String, Double> sortedLibs = new LinkedHashMap<>();
                relevantLibs.entrySet().stream()
                        .sorted(Comparator.comparing(e -> -e.getValue()))
                        .forEach(e -> sortedLibs.put(e.getKey(), e.getValue()));
                int cnt = 0;
                for (Map.Entry<String, Double> entry2 : sortedLibs.entrySet()) {
                    String libId2 = libName2Id.get(entry2.getKey());
                    double weight = entry2.getValue();
                    int idx = (int) (weight * 100) - 1;
                    if (idx >= 0 && idx < relationIds.size()) {
                        String relationId = relationIds.get(idx);
                        bw.write(String.format("%s\t%s\t%s\n", libId1, relationId, libId2));
                    }
                    if (++cnt >= 10) {
                        break;
                    }
                }
            }
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }


}
