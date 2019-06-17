import os
import json
import copy
import logging
import cherrypy
import cherrypy_cors
import random

logging.basicConfig(level=logging.DEBUG)
import argparse
from retriever import ESRetriever, ESBertRetriever, BasicQueryBuilder, SimpleExtendQueryBuilder
from collections import defaultdict
from utils import RemoteSLPTextInference, ResultRecoder, data_loader, infer_local_tokens, deduplicate
from bert_ranking_deployment import init_bert_manager
from utils import ParserUtil
from preprocess.prepare_bert_dataset import build_short_mode_text


class RemoteESServer(object):
  def __init__(self, args):
    snippet_index = 'java-detail-idx'
    snippet_type = 'java-detail-type'

    self.args = args
    self.max_size = 10
    self.do_extend = args.do_extend
    self.extend_token_len = 5

    self.short_args = copy.deepcopy(args)
    self.short_args.state_save_path = args.short_state_save_path
    self.short_args.max_seq_length = args.short_max_seq_length
    self.short_args.batch_size = args.short_batch_size

    self.full_args = copy.deepcopy(args)
    self.full_args.state_save_path = args.full_state_save_path
    self.full_args.max_seq_length = args.full_max_seq_length
    self.full_args.batch_size = args.full_batch_size

    if args.use_bert:
      self.short_bert_manager = init_bert_manager(self.short_args, base_cache_dir='../../ckpt/bert')
      self.full_bert_manager = init_bert_manager(self.full_args, base_cache_dir='../../ckpt/bert')

    self.basic_query_builder = BasicQueryBuilder(fields=['tokenSequence'])
    self.extend_query_builder = SimpleExtendQueryBuilder(fields=['tokenSequence'])
    self.lm_infer = RemoteSLPTextInference(host="127.0.0.1", port=41235)
    self.retriever = ESRetriever(snippet_index, snippet_type)

  @cherrypy.expose(["search_codes"])
  def search_codes(self):
    rawbody = cherrypy.request.body.read(int(cherrypy.request.headers['Content-Length']))
    jsonbody = json.loads(rawbody)
    code_context_tokens = jsonbody['codeContextTokens']
    snippet = jsonbody['snippet']
    user_bert = jsonbody['useBert']

    text_tokens = snippet['tokenSequence']
    if self.do_extend:
      inferred_text_tokens = self.lm_infer.infer(code_context_tokens, text_tokens, self.extend_token_len)
      extend_query = self.extend_query_builder.build_query(text_tokens, inferred_text_tokens, self.max_size * 10)
      search_results = self.retriever.search_snippets(extend_query, with_score=True)
    else:
      basic_query = self.basic_query_builder.build_query(text_tokens, self.max_size * 10)
      search_results = self.retriever.search_snippets(basic_query, with_score=True)

    distinct_results = deduplicate(snippet, search_results, with_score=True)
    if user_bert and self.args.use_bert:
      if len(snippet['lineCodes']) <= 2:
        ## short-bert mode
        # query_snippet_text = build_short_mode_text(snippet)
        # candidate_texts = [build_short_mode_text(res) for res, _ in search_results]
        query_snippet_text = " | ".join(
          [" ".join(ParserUtil.extractNLwords([snippet['className']])),
           " ".join(ParserUtil.extractNLwords([snippet['methodName']]))])
        candidate_texts = [" | ".join([" ".join(ParserUtil.extractNLwords([res['className']])),
                                       " ".join(ParserUtil.extractNLwords([res['methodName']]))])
                           for res, _ in distinct_results]
        scores = self.short_bert_manager.rank(query_snippet_text, candidate_texts)
      else:
        ## full-bert mode
        query_snippet_text = " ".join(ParserUtil.extractNLwords(snippet['tokenSequence']))
        candidate_texts = [" ".join(ParserUtil.extractNLwords(res['tokenSequence']))
                           for res, _ in distinct_results]
        scores = self.full_bert_manager.rank(query_snippet_text, candidate_texts)
      sorted_scores = sorted([(i, score) for i, score in enumerate(scores)], key=lambda d: d[1], reverse=True)

      tmp_indices = []
      for i, score in sorted_scores[:self.max_size]:
        if score >= 0.0:
          tmp_indices.append(i)
        else:
          tmp_index_set = set(tmp_indices)
          for idx in range(min(self.max_size, len(sorted_scores))):
            if idx not in tmp_index_set:
              tmp_indices.append(idx)
          break
      distinct_results = [distinct_results[idx] for idx in tmp_indices]

    distinct_results = distinct_results[:self.max_size]
    distinct_results = [{'methodInfo': res[0], 'score': float(res[1])} for res in distinct_results]

    response = json.dumps(distinct_results)

    print(" ".join(text_tokens))
    print("res size:", len(distinct_results))
    method_ids = [(i + 1, res['methodInfo']['methodId']) for i, res in enumerate(distinct_results)]
    print(method_ids)
    print('=' * 80)

    return response


if __name__ == '__main__':
  cherrypy_cors.install()
  cherrypy.config.update({
    'global': {
      'engine.autoreload.on': False,
      'server.socket_host': '0.0.0.0',
      'server.socket_port': 58362,
      'checker.on': False,
      'tools.log_headers.on': False,
      'request.show_tracebacks': False,
      'request.show_mismatched_params': False,
      'log.screen': False,
      # TODO: add certification file if https support is needed
      # cherrypy.server.ssl_certificate: "cert.pem",
      # cherrypy.server.ssl_private_key: "privkey.pem"
    }
  })

  logging.info("starting searching_manager")
  logging.info("searching_manager started")

  parser = argparse.ArgumentParser()
  parser.add_argument("--do_extend", default=1, type=int, required=False)
  parser.add_argument("--use_bert", default=1, type=int, required=False)
  parser.add_argument("--bert_model", default='bert-base-uncased', type=str, required=False)

  parser.add_argument("--short_state_save_path", default='../../ckpt/bert/rankingckpt/cls+mthName/model.state',
                      type=str)
  parser.add_argument("--short_max_seq_length", default=64, type=int)
  parser.add_argument("--short_batch_size", default=128, type=int)

  parser.add_argument("--full_state_save_path", default='../../ckpt/bert/rankingckpt/fullSeq/model.state', type=str)
  parser.add_argument("--full_max_seq_length", default=256, type=int)
  parser.add_argument("--full_batch_size", default=64, type=int)
  args = parser.parse_args()

  cherrypy.quickstart(RemoteESServer(args),
                      config={'/': {
                        'cors.expose.on': True,
                      }})
