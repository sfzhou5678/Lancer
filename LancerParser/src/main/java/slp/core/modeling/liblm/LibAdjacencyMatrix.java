package slp.core.modeling.liblm;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import slp.core.util.Pair;
import utils.FileUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class LibAdjacencyMatrix {
    private boolean directedGraph = false;
    public Map<String, Map<String, Double>> matrix;

    public LibAdjacencyMatrix(boolean directedGraph) {
        matrix = new HashMap<>();
        this.directedGraph = directedGraph;
    }

    public LibAdjacencyMatrix(boolean directedGraph, Map<String, Map<String, Double>> matrix) {
        this.directedGraph = directedGraph;
        this.matrix = matrix;
    }

    public void insertEdge(String libName1, String libName2, Double weight) {
        doInsertEdge(libName1, libName2, weight);
        if (!this.directedGraph) {
            doInsertEdge(libName2, libName1, weight);
        }
    }

    public void insertEdge(String libName1, String libName2) {
        insertEdge(libName1, libName2, (double) 1);
    }

    private void doInsertEdge(String libName1, String libName2, Double weight) {
        if (!matrix.containsKey(libName1)) {
            matrix.put(libName1, new HashMap<String, Double>());
        }
        Map<String, Double> edges = matrix.get(libName1);
        if (!edges.containsKey(libName2)) {
            edges.put(libName2, (double) 0);
        }
        edges.put(libName2, edges.get(libName2) + weight);
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K, V>> st = map.entrySet().stream();

        st.sorted(Comparator.comparing(e -> e.getValue())).forEach(e -> result.put(e.getKey(), e.getValue()));

        return result;
    }

    public HashMap<String, Double> getTopK(String libName, int k) {
        HashMap<String, Double> map = new HashMap<>();
        if (matrix.containsKey(libName)) {
            LinkedHashMap sortedLibs = (LinkedHashMap) sortByValue(matrix.get(libName));

            ListIterator<Map.Entry<String, Double>> i = new ArrayList<Map.Entry<String, Double>>(sortedLibs.entrySet()).listIterator(sortedLibs.size());
            int cnt = 0;
            while (i.hasPrevious() && cnt++ < k) {
                Map.Entry<String, Double> entry = i.previous();
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    class WriteableEdges {
        private String libName;
        private Map<String, Double> edges;

        public WriteableEdges(String libName, Map<String, Double> edges) {
            this.libName = libName;
            this.edges = edges;
        }

        public String getLibName() {
            return libName;
        }

        public void setLibName(String libName) {
            this.libName = libName;
        }

        public Map<String, Double> getEdges() {
            return edges;
        }

        public void setEdges(Map<String, Double> edges) {
            this.edges = edges;
        }
    }

    public static LibAdjacencyMatrix load(String libAdjacencyMatrixSavePath) {
        FileReader fr = null;
        try {
            fr = new FileReader(libAdjacencyMatrixSavePath);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();

            Gson gson = new Gson();
            LibAdjacencyMatrix matrix = new LibAdjacencyMatrix(Boolean.valueOf(line));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                WriteableEdges writeableEdges = gson.fromJson(line, WriteableEdges.class);

                matrix.matrix.put(writeableEdges.getLibName(), writeableEdges.getEdges());
            }
            br.close();
            fr.close();
            return matrix;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean save(String libAdjacencyMatrixSavePath) {
        File fout = new File(libAdjacencyMatrixSavePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(directedGraph + "\n");
            for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
                Map<String, Double> edges = matrix.get(entry.getKey());
                WriteableEdges writeableEdges = new WriteableEdges(entry.getKey(), edges);

                String content = JSON.toJSONString(writeableEdges);
                bw.write(content + "\n");
            }
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
