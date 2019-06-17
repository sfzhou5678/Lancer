package slp.core.modeling.liblm;

import slp.core.lexing.LexerRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LibIdentifier {
    private LibTrie trie;

    public LibIdentifier() {
        this.trie = new LibTrie();
    }

    public LibIdentifier(LibTrie trie) {
        this.trie = trie;
    }


    public void learnFile(File file) {
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

        trie.insertLibs(libTokens);
    }

    public void learnDir(File dir) {
        long[] learnCounts = new long[]{0, 0};
        long[] learnTime = new long[]{-System.currentTimeMillis()};
        try {
            Files.walk(dir.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .peek(f -> {
                        if (++learnCounts[0] % 1000 == 0) {
                            System.out.println("Counting at file " + learnCounts[0] + " in " + (learnTime[0] + System.currentTimeMillis()) / 1000 + "s");
                        }
                    })
                    .forEach(f -> learnFile(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public List<List<String>> identifyLibs() {
        return trie.identifyLibs();
    }

    @Override
    public String toString() {
        return this.trie.toString();
    }
}
