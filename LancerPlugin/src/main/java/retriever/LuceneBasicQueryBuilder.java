package retriever;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import slp.core.infos.MethodInfo;
import utils.ParserUtil;

import java.util.Arrays;

public class LuceneBasicQueryBuilder {
    private static String field = "keywordSequence";

    public static Query buildQuery(MethodInfo currentMethod) {
        Query query = null;
        QueryParser parser = new QueryParser(LuceneBasicQueryBuilder.field, new StandardAnalyzer());
        try {
            String tokenText = String.join(" ", currentMethod.getTokenSequence());
            tokenText = String.join(" ", ParserUtil.extractNLwords(Arrays.asList(tokenText.split(" ", 0))));
            query = parser.parse(tokenText);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return query;
    }
}
