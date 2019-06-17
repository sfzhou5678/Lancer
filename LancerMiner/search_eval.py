import json
import argparse
from tqdm import tqdm
from retriever import ESRetriever, ESBertRetriever, BasicQueryBuilder, CombineQueryBuilder
from retriever import ExtendQueryBuilder, SimpleExtendQueryBuilder, BoolExtendQueryBuilder

from collections import defaultdict
from utils import IdealInference, SLPTextInference, ResultRecoder, data_loader, infer_local_tokens, deduplicate
from utils import RemoteSLPTextInference, RemoteLMInference
from bert_ranking_deployment import init_bert_manager
from utils import ParserUtil
from preprocess.prepare_bert_dataset import build_short_mode_text

if __name__ == '__main__':
  split_type = 'ccl-split'
  clone_pairs_path = 'data/clone/bcb-EntireTrueClonePairList.txt'
  file_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\%s\test-fileInfos.txt" % split_type
  method_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\%s\test-methodInfos-withAPI.txt" % split_type

  line_limit = None
  clone_dict = data_loader.load_clone_dict(clone_pairs_path, label_propagation=None)
  method_infos = data_loader.load_method_infos(method_infos_path, line_limit)

  # snippet_index = 'texture-lvl-idx'
  # snippet_type = 'texture-lvl-type'
  # query_builder = BasicQueryBuilder(fields=['tokenSequence'])

  snippet_index = 'mix-detail-idx'
  snippet_type = 'mix-detail-type'
  basic_query_builder = BasicQueryBuilder(fields=['tokenSequence'])
  extend_query_builder = SimpleExtendQueryBuilder(fields=['tokenSequence'])
  # query_builder = BoolExtendQueryBuilder(fields=['tokenSequence'])
  retriever = ESRetriever(snippet_index, snippet_type)

  use_extend = True
  user_bert = False
  short_mode = False
  if user_bert:
    if short_mode:
      short_parser = argparse.ArgumentParser()
      short_parser.add_argument("--bert_model", default='bert-base-uncased', type=str, required=False)
      # short_parser.add_argument("--state_save_path", default='ckpt/bert/rankingckpt/cls+mthName/model.state', type=str)
      short_parser.add_argument("--state_save_path",
                                default='ckpt/bert/rankingckpt/%s/short-ratio-5/model.state' % split_type, type=str)
      short_parser.add_argument("--max_seq_length", default=64, type=int)
      short_parser.add_argument("--batch_size", default=128, type=int)
      short_args = short_parser.parse_args()
      short_bert_manager = init_bert_manager(short_args, base_cache_dir='ckpt/bert')
    else:
      full_parser = argparse.ArgumentParser()
      full_parser.add_argument("--bert_model", default='bert-base-uncased', type=str, required=False)
      # full_parser.add_argument("--state_save_path",
      #                          default='ckpt/bert/rankingckpt/fullSeq/model.state', type=str, required=False)
      full_parser.add_argument("--state_save_path",
                               default='ckpt/bert/rankingckpt/%s/full-ratio-5/model.state' % split_type, type=str)
      full_parser.add_argument("--max_seq_length", default=256, type=int)
      full_parser.add_argument("--batch_size", default=128, type=int)
      full_args = full_parser.parse_args()
      full_bert_manager = init_bert_manager(full_args, base_cache_dir='ckpt/bert')

  max_size = 10
  token_ratio = 5
  extend_token_len = 5
  # lm_infer = IdealInference()
  lm_infer = RemoteSLPTextInference(host="127.0.0.1", port=41235)

  print('use_extend:', use_extend)
  print('use_bert:', user_bert)
  print('short_mode:', short_mode)

  print("max_size:", max_size)
  print("token_ratio:", token_ratio)
  print("extend_token_len:", extend_token_len)
  print("lm_infer:", lm_infer)

  if isinstance(lm_infer, RemoteLMInference):
    file_infos = data_loader.load_method_infos(file_infos_path, None)
    file_dict = {file_info['fileId']: file_info for file_info in file_infos}
    del file_infos

  cnt = 0
  res_recoder = ResultRecoder(clone_dict, k_list=[1, 5, 10], clone_type_list=['T1', 'T2', 'ST3', 'MT3'])
  for snippet in tqdm(method_infos):
    if snippet['methodId'] in clone_dict:
      cnt += 1
      if token_ratio > 1:
        token_length = len(snippet['tokenSequence']) // token_ratio
      else:
        token_length = len(snippet['tokenSequence'])

      text_tokens = snippet['tokenSequence'][: token_length]
      if use_extend:
        if isinstance(lm_infer, RemoteLMInference):
          if snippet['affiliatedFileId'] not in file_dict:
            inferred_text_tokens = []
          else:
            file_info = file_dict[snippet['affiliatedFileId']]
            tmp_tokens = file_info['tokens']
            for idx in file_info['commentsTokenIndex']:
              tmp_tokens[idx] = ""
            code_context_tokens = tmp_tokens[:snippet['startIndexInFile'] + token_length]
            inferred_text_tokens = lm_infer.infer(code_context_tokens, text_tokens, extend_token_len)
        else:
          inferred_text_tokens = lm_infer.infer(snippet, token_length, extend_token_len, 'tokenSequence')

        target_tokens = snippet['tokenSequence'][token_length:token_length + extend_token_len]  # used for debug
        ## mode2, it seems that mode2 slightly outperforms mode 1 in T3+
        extend_query = extend_query_builder.build_query(text_tokens, inferred_text_tokens, max_size * 10)
        search_results = retriever.search_snippets(extend_query)

        # ## mode 1
        # basic_query = basic_query_builder.build_query(text_tokens, max_size * 5)
        # extend_query = extend_query_builder.build_query(text_tokens, inferred_text_tokens, max_size * 5)
        # search_results = retriever.search_snippets(basic_query) + retriever.search_snippets(extend_query)

      else:
        basic_query = basic_query_builder.build_query(text_tokens, max_size * 10)
        search_results = retriever.search_snippets(basic_query)
      search_results = deduplicate(snippet, search_results)

      if user_bert:
        if short_mode:
          ## short-bert mode
          query_snippet_text = build_short_mode_text(snippet)
          candidate_texts = [build_short_mode_text(res) for res in search_results]
          scores = short_bert_manager.rank(query_snippet_text, candidate_texts)
        else:
          ## full-bert mode
          query_snippet_text = " ".join(ParserUtil.extractNLwords(text_tokens))
          candidate_texts = [" ".join(ParserUtil.extractNLwords(res['tokenSequence']))
                             for res in search_results]
          scores = full_bert_manager.rank(query_snippet_text, candidate_texts)
        sorted_scores = sorted([(i, score) for i, score in enumerate(scores)], key=lambda d: d[1], reverse=True)
        # 如果bert得分不高，就返回文本匹配的结果
        tmp_indices = []
        for i, score in sorted_scores[:max_size]:
          if score >= 0.4:
            tmp_indices.append(i)
          else:
            tmp_index_set = set(tmp_indices)
            for idx in range(min(max_size, len(sorted_scores))):
              if idx not in tmp_index_set:
                tmp_indices.append(idx)
            break
        search_results = [search_results[idx] for idx in tmp_indices]
      search_results = search_results[:max_size]
      res_recoder.record(snippet, search_results)
      pass
  print(cnt)
  ## 输出结果
  res_recoder.describe()
