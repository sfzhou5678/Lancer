package retriever;

import indexer.LuceneIndexer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import slp.core.infos.MethodInfo;
import utils.ParserUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LuceneRetriever {
    private static LuceneRetriever retriever;

    private IndexReader reader;
    private IndexSearcher searcher;

    private LuceneRetriever() {
        init();
    }

    public static LuceneRetriever getRetriever() {
        if (retriever == null) {
            retriever = new LuceneRetriever();
        }
        return retriever;
    }

    public void init() {
        try {
            Directory directory = FSDirectory.open(Paths.get(LuceneIndexer.indexSaveDir));
            this.reader = DirectoryReader.open(directory);
            this.searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<MethodInfo> search(Query query) {
        return search(query, 10);
    }

    public List<MethodInfo> search(Query query, int n) {
        if (this.searcher == null) {
            return null;
        }
        try {
            TopDocs topDocs = searcher.search(query, n);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            List<MethodInfo> methodInfos = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                int docId = scoreDoc.doc;
                Document doc = searcher.doc(docId);
                MethodInfo methodInfo = new MethodInfo(doc.get("methodId"), doc.get("methodName"), doc.get("className"), doc.get("returnType"),
                        Arrays.asList(doc.get("paramTypes").split(" ", 0)), Arrays.asList(doc.get("lineCodes").split("\n", 0)));
                if (methodInfo != null && ParserUtil.extractNLwords(Arrays.asList(methodInfo.getMethodName())).size() > 0) {
                    methodInfos.add(methodInfo);
                }
            }
            return methodInfos;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (this.reader != null)
            reader.close();
    }
}
