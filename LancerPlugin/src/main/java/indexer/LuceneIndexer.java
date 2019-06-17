package indexer;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import retriever.LuceneRetriever;
import slp.core.infos.FileInfo;
import slp.core.infos.InfoCollector;
import slp.core.infos.MethodInfo;
import slp.core.io.Reader;
import slp.core.lexing.code.JavaDetailLexer;
import utils.ParserUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LuceneIndexer {
    private static LuceneIndexer indexer;
    public static String indexSaveDir = "LAMPLuceneIdxDir";
    private static String updateFileQueueSavePath = "LAMPLuceneIdxDir/updateFile.queue";

    private Directory directory = null;
    private Analyzer analyzer = null;
    private IndexWriterConfig config = null;
    private IndexWriter indexWriter = null;

    private JavaDetailLexer lexer = null;

    private LuceneIndexer() {
        // FIXME: 2019/5/20 should Indexer be a service rather than a class?
        lexer = new JavaDetailLexer();
    }

    public static LuceneIndexer getIndexer() {
        if (indexer == null) {
            indexer = new LuceneIndexer();
        }
        return indexer;
    }

    private boolean init() {
        try {
            directory = FSDirectory.open(Paths.get(LuceneIndexer.indexSaveDir));
            analyzer = new StandardAnalyzer();
            config = new IndexWriterConfig(analyzer);
            indexWriter = new IndexWriter(directory, config);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void updateFile(File file) {
        // TODO: Write-Ahead Logging, read file path from fileQueue, and update methods in the files (if existed -> update, else -> add)
    }

    public static boolean deleteDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }

        String[] content = file.list();
        for (String name : content) {
            File temp = new File(path, name);
            if (temp.isDirectory()) {
                deleteDir(temp.getAbsolutePath());
                temp.delete();
            } else {
                if (!temp.delete()) {
                }
            }
        }
        return true;
    }

    public void indexDir(File dir) {
        // FIXME: 2019/5/20 add a check step
        deleteDir(Paths.get(LuceneIndexer.indexSaveDir).toAbsolutePath().toString());

        new Thread(() -> {
            // TODO: 2019/5/19 Write-Ahead Logging, records the files needed process & processed files
            try {
                Files.walk(dir.toPath())
                        .map(Path::toFile)
                        .filter(File::isFile)
                        .forEach(f -> indexFile(f, false));
                LuceneRetriever.getRetriever().init();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                this.indexWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void indexFile(File file, boolean needCloseWriter) {
        indexFile(file);
        if (needCloseWriter) {
            try {
                this.indexWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void indexFile(File file) {
        if (file == null || !file.getName().endsWith(".java")) {
            return;
        }

        if (this.indexWriter == null || !this.indexWriter.isOpen()) {
            if (!init()) {
                System.out.println("Indexer init error.");
                return;
            }
        }
        // FIXME: 2019/5/19 read methods from file , care of params of SLP core!!!!
        FileInfo fileInfo = new FileInfo(file.getName(), "", file.getAbsolutePath());
        String fileId = DigestUtils.md5Hex(fileInfo.getBaseFolderPath() + fileInfo.getRelativeFilePath() + fileInfo.getAffiliatedProject());
        fileInfo.setFileId(fileId);

        String lexedDetail = lexer.lexJson(fileInfo, Reader.readLines(file));
        List<MethodInfo> methodInfos = JSON.parseObject(lexedDetail, InfoCollector.class).getMethodInfoList();

        for (MethodInfo info : methodInfos) {
            Document document = new Document();

            Field methodIdField = new StringField("methodId", info.getMethodId(), Field.Store.YES);
            Field classNameField = new StringField("className", info.getClassName(), Field.Store.YES);
            Field methodNameField = new StringField("methodName", info.getMethodName(), Field.Store.YES);
            Field returnTypeField = new StringField("returnType", info.getReturnType(), Field.Store.YES);
            Field paramTypesField = new TextField("paramTypes", String.join(" ", info.getParamTypes()), Field.Store.YES);
            Field tokenSequenceField = new TextField("tokenSequence",
                    String.join(" ", ParserUtil.filterReversedWords(info.getTokenSequence())), Field.Store.YES);
            Field keywordSequenceField = new TextField("keywordSequence",
                    String.join(" ", ParserUtil.extractNLwords(info.getTokenSequence())), Field.Store.YES);

            Field lineCodes = new StoredField("lineCodes", String.join("\n", info.getLineCodes()));
            Field startIndexInFile = new StoredField("startIndexInFile", info.getStartIndexInFile());
            Field endIndexInFile = new StoredField("endIndexInFile", info.getEndIndexInFile());
            Field startLine = new StoredField("startLine", info.getStartLine());
            Field endLine = new StoredField("endLine", info.getEndLine());

            document.add(methodIdField);
            document.add(classNameField);
            document.add(methodNameField);
            document.add(returnTypeField);
            document.add(paramTypesField);
            document.add(tokenSequenceField);
            document.add(keywordSequenceField);

            document.add(lineCodes);
            document.add(startIndexInFile);
            document.add(endIndexInFile);
            document.add(startLine);
            document.add(endLine);
            try {
                indexWriter.addDocument(document);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
