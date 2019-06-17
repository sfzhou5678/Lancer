import os
import json
import random
import numpy as np
from collections import defaultdict
from utils import IdealInference, SLPTextInference, ResultRecoder, data_loader, get_clone_type
from retriever import ESRetriever, BasicQueryBuilder, CombineQueryBuilder
from tqdm import tqdm
from utils import infer_local_tokens
from utils.search_utils import *


def prepare_bm25_bcb_dataset(save_folder, lm_infer, query_builder, retriever, max_size, extend_token_len,
                             token_ratio=1, token_length=50, is_test=True):
  raw_max_size = max_size
  qid = 1000000
  if is_test:
    test_fw = open(os.path.join(save_folder, 'test.txt'), 'w', encoding='utf-8')
  else:
    train_fw = open(os.path.join(save_folder, 'train.txt'), 'w', encoding='utf-8')
    valid_fw = open(os.path.join(save_folder, 'valid.txt'), 'w', encoding='utf-8')
  for method_info in tqdm(method_infos):
    if method_info['methodId'] in clone_dict:
      text_tokens, inferred_text_tokens = infer_local_tokens(method_info, 'tokenSequence', lm_infer, token_length,
                                                             token_ratio, extend_token_len)
      method_info['inferredTokenSequence'] = inferred_text_tokens
      if isinstance(query_builder, CombineQueryBuilder):
        api_tokens, inferred_api_tokens = infer_local_tokens(
          method_info, 'apiSequence', lm_infer, token_length, token_ratio // 3,
          round(extend_token_len * (len(method_info['apiSequence']) / (len(method_info['tokenSequence']) + 1))))
        method_info['inferredApiSequence'] = inferred_api_tokens
        query = query_builder.build_query(text_tokens, api_tokens, max_size * 5)
      else:
        query = query_builder.build_query(text_tokens, max_size * 5)
      search_results = retriever.search_snippets(query, with_score=True)

      qid += 1
      distinct_methods = []
      group = []
      ## 1. add the manually labeled GT clones
      for i, (cloned_method_info, score) in enumerate(search_results):
        cur_clone_type = get_clone_type(clone_dict, method_info['methodId'], cloned_method_info['methodId'])
        label = clone_type_to_label[cur_clone_type] if cur_clone_type in clone_type_to_label else 0
        if label > 0:
          if label >= 2 and is_duplicated(cloned_method_info, distinct_methods):
            continue
          sample = {'qid': qid, 'label': label,
                    'methodId1': str(method_info['methodId']), 'methodId2': str(cloned_method_info['methodId'])}
          group.append(sample)
          distinct_methods.append(cloned_method_info)

      max_size = max(raw_max_size, len(group) * 3)
      ## 2. deduplicate some unlabeled but similar codes, prevent producing confusing labels (for the similar snippets)
      for i, (cloned_method_info, score) in enumerate(search_results):
        cur_clone_type = get_clone_type(clone_dict, method_info['methodId'], cloned_method_info['methodId'])
        label = clone_type_to_label[cur_clone_type] if cur_clone_type in clone_type_to_label else 0
        if label == 0:
          if not is_duplicated(cloned_method_info, distinct_methods):
            sample = {'qid': qid, 'label': label,
                      'methodId1': str(method_info['methodId']), 'methodId2': str(cloned_method_info['methodId'])}
            group.append(sample)
            distinct_methods.append(cloned_method_info)
            if len(group) >= max_size:
              break

      random.shuffle(group)
      if is_test:
        fw = test_fw
      else:
        t = random.random()
        if t < 0.8:
          fw = train_fw
        else:
          fw = valid_fw
      for sample in group:
        fw.write(json.dumps(sample, ensure_ascii=False) + '\n')
      fw.flush()
  if is_test:
    test_fw.close()
  else:
    train_fw.close()
    valid_fw.close()


if __name__ == '__main__':
  clone_type_to_label = {
    'T1': 2,
    'T2': 2,
    'ST3': 1,
    'MT3': 1,
    'T4': 1,
    'other': 0
  }
  clone_pairs_path = 'data/clone/bcb-EntireTrueClonePairList.txt'
  clone_dict = data_loader.load_clone_dict(clone_pairs_path, label_propagation=None)

  split_type = "random0.5-split"
  is_test = False
  method_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\%s\%s-methodInfos-withAPI.txt" % (
    split_type, "test" if is_test else "train")
  line_limit = None
  method_infos = data_loader.load_method_infos(method_infos_path, line_limit)

  token_length = 50
  mode = 'ratio'
  token_ratio = 5
  extend_token_len = 3
  sample_num = 10
  save_folder = 'data/ltr/bcb/elasticsearch/%s/ratio-%d/sample_num%d' % (split_type, token_ratio, sample_num)
  if not os.path.exists(save_folder):
    os.makedirs(save_folder)

  snippet_index = 'mix-detail-idx'
  snippet_type = 'mix-detail-type'
  query_builder = CombineQueryBuilder(text_fields=['tokenSequence'], api_fields=['apiSequence'])

  retriever = ESRetriever(snippet_index, snippet_type)

  lm_infer = IdealInference()
  # lm_infer = SLPTextInference(host="127.0.0.1", port=41235)
  prepare_bm25_bcb_dataset(save_folder, lm_infer, query_builder, retriever, sample_num, extend_token_len,
                           token_ratio, token_length, is_test)
