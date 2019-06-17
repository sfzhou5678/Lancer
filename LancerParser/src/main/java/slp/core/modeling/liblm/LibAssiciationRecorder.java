package slp.core.modeling.liblm;

import slp.core.lexing.LexerRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LibAssiciationRecorder {
    LibTrie libNameTrie;
    LibAdjacencyMatrix libAdjacencyMatrix;

    public LibAssiciationRecorder(LibTrie libNameTrie) {
        this.libNameTrie = libNameTrie;
        this.libAdjacencyMatrix = new LibAdjacencyMatrix(false);
    }

    public LibAssiciationRecorder(LibTrie libNameTrie, LibAdjacencyMatrix libAdjacencyMatrix) {
        this.libNameTrie = libNameTrie;
        this.libAdjacencyMatrix = libAdjacencyMatrix;
    }

    public void recordFile(File file) {
        // 1. parse given file
        Stream<Stream<String>> lineStream = LexerRunner.lex(file);
        List<Stream<String>> tmp = lineStream.collect(Collectors.toList());
        List<List<String>> lineTokens = new ArrayList<>();
        for (Stream<String> s : tmp) {
            lineTokens.add(s.collect(Collectors.toList()));
        }

        // 2. extract imported libs
        List<List<String>> libTokens = new ArrayList<>();
        for (List<String> line : lineTokens) {
            if (line.size() > 0 && line.get(0).equals("import") && line.get(line.size() - 1).equals(";")) {
                line = line.subList(1, line.size() - 1);
                libTokens.add(line);
            } else if (libTokens.size() > 0 && line.size() > 2) {
                // simply assume that import only occurs in the front of files.
                break;
            }
        }

        Set<String> invokedLibNameSet = new HashSet<>();
        for (List<String> tokens : libTokens) {
            List<String> libToken = libNameTrie.search(tokens);
            if (libToken != null) {
                invokedLibNameSet.add(String.join(".", libToken));
            }
        }
        List<String> invokedLibNames = new ArrayList<>();
        invokedLibNames.addAll(invokedLibNameSet);
        for (int i = 0; i < invokedLibNames.size(); i++) {
            String libName1 = invokedLibNames.get(i);
            for (int j = i + 1; j < invokedLibNames.size(); j++) {
                String libName2 = invokedLibNames.get(j);
                libAdjacencyMatrix.insertEdge(libName1, libName2);
            }
        }
    }

    public void recordDir(File dir) {
        long[] learnCounts = new long[]{0, 0};
        long[] learnTime = new long[]{-System.currentTimeMillis()};
        try {
            Files.walk(dir.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .peek(f -> {
                        if (++learnCounts[0] % 1000 == 0) {
                            System.out.println("Recording at file " + learnCounts[0] + " in " + (learnTime[0] + System.currentTimeMillis()) / 1000 + "s");
                        }
                    })
                    .forEach(f -> recordFile(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LibAdjacencyMatrix getLibAdjacencyMatrix() {
        return libAdjacencyMatrix;
    }
}
