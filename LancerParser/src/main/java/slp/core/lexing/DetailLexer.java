package slp.core.lexing;

import slp.core.infos.FileInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface DetailLexer {
    default String lexJson(FileInfo fileInfo, Stream<String> lines) {
        return lexJson(fileInfo, lines.collect(Collectors.toList()));
    }

    String lexJson(FileInfo fileInfo, List<String> lines);
}
