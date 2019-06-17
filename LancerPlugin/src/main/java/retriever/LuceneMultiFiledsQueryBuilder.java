package retriever;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import slp.core.infos.MethodInfo;
import utils.ParserUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LuceneMultiFiledsQueryBuilder {
    public static Query buildQuery(MethodInfo currentMethod) {
        Query query = null;
        QueryParser seqParser = new QueryParser("tokenSequence", new StandardAnalyzer());
        QueryParser keywordParser = new QueryParser("keywordSequence", new StandardAnalyzer());
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        try {
            String tokenText = String.join(" ", currentMethod.getTokenSequence());

            Query nameQuery = new TermQuery(new Term("methodName", currentMethod.getMethodName()));
            Query seqQuery = seqParser.parse(String.join(" ", ParserUtil.filterReversedWords(Arrays.asList(tokenText.split(" ", 0)))));
            Query keyQuery = keywordParser.parse(String.join(" ", ParserUtil.extractNLwords(Arrays.asList(tokenText.split(" ", 0)))));

            booleanQuery.add(nameQuery, BooleanClause.Occur.SHOULD);
            booleanQuery.add(seqQuery, BooleanClause.Occur.SHOULD);
            booleanQuery.add(new BoostQuery(keyQuery, (float) 0.25), BooleanClause.Occur.SHOULD);
            query = booleanQuery.build();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return query;
    }
}
