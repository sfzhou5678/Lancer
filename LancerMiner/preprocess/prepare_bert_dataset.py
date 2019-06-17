import os
import json
import copy
from utils import data_loader, ParserUtil


def build_short_mode_text(method_info):
  method_text_tokens = " ".join(ParserUtil.extractNLwords([method_info['methodName']]))
  if method_text_tokens == "":
    return ""

  class_text_tokens = " ".join(ParserUtil.extractNLwords([method_info['className']]))
  return_type_text_tokens = method_info['returnType'].lower()
  param_type_text_tokens = " , ".join(
    [" ".join(ParserUtil.extractNLwords([param_type])) for param_type in method_info['paramTypes']]
  )

  text = " | ".join([class_text_tokens, method_text_tokens, return_type_text_tokens, param_type_text_tokens])
  return text


def process(method_info_dict, data, key1, key2, wf, short_mode):
  if data[key1] not in method_info_dict or data[key2] not in method_info_dict:
    return
  data['textA'] = build_short_mode_text(method_info_dict[data[key1]])
  data['textB'] = build_short_mode_text(method_info_dict[data[key2]])

  if (data['textA'] != "") and (data['textB'] != ""):
    # the short mode is necessary
    wf.write('%s\n' % json.dumps(data, ensure_ascii=False))
  if not short_mode:
    seq1 = method_info_dict[data[key1]]['tokenSequence']
    seq2 = method_info_dict[data[key2]]['tokenSequence']
    for ratio in [1, 2, 3, 5, 10]:
      text_a = " ".join(ParserUtil.extractNLwords(seq1[:len(seq1) // ratio]))
      text_b = " ".join(ParserUtil.extractNLwords(seq2))
      if not ((text_a == "" or text_a.endswith("| ")) or (text_b == "" or text_b.endswith("| "))):
        data_copy = copy.deepcopy(data)
        data_copy['textA'] = text_a
        data_copy['textB'] = text_b
        data_copy['ratio'] = ratio
        wf.write('%s\n' % json.dumps(data_copy, ensure_ascii=False))


def build_bert_data(input_pair_path, save_path, method_info_dict, short_mode=True, print_freq=10000):
  processed_pair_set = set()
  with open(input_pair_path) as f, open(save_path, 'w', encoding='utf-8') as wf:
    lines = f.readlines()
    cnt = 0
    for line in lines:
      data = json.loads(line)
      if 'features' in data:
        data.pop('features')
      if data['methodId1'] == data['methodId2']:
        continue
      if (data['methodId1'], data['methodId2']) not in processed_pair_set:
        processed_pair_set.add((data['methodId1'], data['methodId2']))
        process(method_info_dict, data, "methodId1", "methodId2", wf, short_mode)
      if (data['methodId2'], data['methodId1']) not in processed_pair_set:
        processed_pair_set.add((data['methodId2'], data['methodId1']))
        process(method_info_dict, data, "methodId2", "methodId1", wf, short_mode)
      cnt += 1
      if cnt % print_freq == 0:
        print('%d / %d' % (cnt, len(lines)))
    try:
      pass
    except Exception as e:
      pass


if __name__ == '__main__':
  split_type = "ccl-split"
  pair_data_folder = '../data/ltr/bcb/elasticsearch/%s/ratio-5/sample_num10' % (split_type)
  short_mode = True
  is_test = True
  if is_test:
    method_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\%s\test-methodInfos-withAPI.txt" % (
      split_type)
    tags = ['test']
  else:
    method_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\%s\train-methodInfos-withAPI.txt" % (
      split_type)
    tags = ['train', 'valid']

  line_limit = None
  method_infos = data_loader.load_method_infos(method_infos_path, line_limit)
  method_info_dict = {info['methodId']: info for info in method_infos}

  bert_save_folder = '../data/ltr/bcb/%s/bert-%s-ratio-5' % (split_type, "short" if short_mode else "full")
  if not os.path.exists(bert_save_folder):
    os.makedirs(bert_save_folder)

  data_set = {tag: build_bert_data(input_pair_path=os.path.join(pair_data_folder, '%s.txt' % tag),
                                   save_path=os.path.join(bert_save_folder, '%s.txt' % tag),
                                   method_info_dict=method_info_dict, short_mode=short_mode)
              for tag in tags}
