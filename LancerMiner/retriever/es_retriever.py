from elasticsearch import Elasticsearch
from scipy import stats
import numpy as np
from utils import ParserUtil


class ESRetriever(object):
  def __init__(self, index, type,
               url='localhost:9200'):
    self.index = index
    self.type = type

    self.es = Elasticsearch(url)

  def search_snippets(self, query, with_score=False):
    search_results = self.es.search(index=[self.index], doc_type=self.type, body=query)
    search_results = search_results['hits']['hits']

    if with_score:
      scores = [s['_score'] for s in search_results]
      # scores = stats.zscore(scores)
      scores = np.nan_to_num(scores)

      search_results = [(res['_source'], score) for res, score in zip(search_results, scores)]
    else:
      search_results = [s['_source'] for s in search_results]

    return search_results


class ESBertRetriever(ESRetriever):
  def __init__(self, index, type, bert_manager, short_mode, url='localhost:9200'):
    super().__init__(index, type, url)
    self.bert_manager = bert_manager
    self.short_mode = short_mode

  def search_snippets(self, query, with_score=False, cur_snippet=None):
    search_results = self.es.search(index=[self.index], doc_type=self.type, body=query)
    search_results = search_results['hits']['hits']

    if self.short_mode:
      query_snippet_text = " | ".join(
        [" ".join(ParserUtil.extractNLwords([cur_snippet['className']])),
         " ".join(ParserUtil.extractNLwords([cur_snippet['methodName']]))])
      candidate_texts = [" | ".join([" ".join(ParserUtil.extractNLwords([res['_source']['className']])),
                                     " ".join(ParserUtil.extractNLwords([res['_source']['methodName']]))])
                         for res in search_results]
    else:
      if "multi_match" in query['query']:
        # BasicQueryBuilder
        query_snippet_text = query['query']['multi_match']['query']
      elif "bool" in query['query']:
        # CombineQueryBuilder
        query_snippet_text = query['query']['bool']['should'][0]['multi_match']['query']
      else:
        raise Exception()
      candidate_texts = [" ".join(res['_source']['tokenSequence'])
                         for res in search_results]

    scores = self.bert_manager.rank(query_snippet_text, candidate_texts)
    sorted_scores = sorted([(i, score) for i, score in enumerate(scores)], key=lambda d: d[1], reverse=True)

    if with_score:
      search_results = [(search_results[i]['_source'], score) for i, score in sorted_scores]
    else:
      search_results = [search_results[i]['_source'] for i, score in sorted_scores]

    return search_results
