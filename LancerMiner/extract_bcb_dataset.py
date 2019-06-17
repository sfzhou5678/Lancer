import json
import random
from utils import data_loader


def ccl_split():
  train_file_set = set()
  test_file_set = set()

  with open('train-fileInfos.txt', 'w', encoding='utf-8') as train_fw, \
      open('test-fileInfos.txt', 'w', encoding='utf-8') as test_fw:
    for file_info in file_infos:
      func_type = int(file_info['relativeFilePath'].split('\\')[0])
      if func_type == 4:
        train_file_set.add(file_info['fileId'])
        train_fw.write('%s\n' % json.dumps(file_info, ensure_ascii=False))
      else:
        test_file_set.add(file_info['fileId'])
        test_fw.write('%s\n' % json.dumps(file_info, ensure_ascii=False))

  with open('train-methodInfos-withAPI.txt', 'w', encoding='utf-8') as train_fw, \
      open('test-methodInfos-withAPI.txt', 'w', encoding='utf-8') as test_fw:
    for method_info in method_infos:
      if method_info['affiliatedFileId'] in train_file_set:
        train_fw.write('%s\n' % json.dumps(method_info, ensure_ascii=False))
      else:
        test_fw.write('%s\n' % json.dumps(method_info, ensure_ascii=False))


def random_split(train_ratio=0.5):
  clone_pairs_path = 'data/clone/bcb-EntireTrueClonePairList.txt'
  clone_dict = data_loader.load_clone_dict(clone_pairs_path, label_propagation=None)

  total_pair_cnt = 0
  labeled_method_set = set()
  for mid1 in clone_dict:
    labeled_method_set.add(mid1)
    for mid2 in clone_dict[mid1]:
      labeled_method_set.add(mid2)
      total_pair_cnt += 1
  print(len(labeled_method_set))

  train_method_id_set = set()
  test_method_id_set = set()
  for mid in labeled_method_set:
    t = random.random()
    if t < train_ratio:
      train_method_id_set.add(mid)
    else:
      test_method_id_set.add(mid)

  file_dict = {file_info['fileId']: file_info for file_info in file_infos}

  with open('train-methodInfos-withAPI.txt', 'w', encoding='utf-8') as train_fw, \
      open('test-methodInfos-withAPI.txt', 'w', encoding='utf-8') as test_fw:
    with open('train-fileInfos.txt', 'w', encoding='utf-8') as train_file_fw, \
        open('test-fileInfos.txt', 'w', encoding='utf-8') as test_file_fw:
      for method_info in method_infos:
        if method_info['methodId'] in train_method_id_set:
          if method_info['affiliatedFileId'] in file_dict:
            train_fw.write('%s\n' % json.dumps(method_info, ensure_ascii=False))
            train_file_fw.write('%s\n' % json.dumps(file_dict[method_info['affiliatedFileId']], ensure_ascii=False))
        elif method_info['methodId'] in test_method_id_set:
          if method_info['affiliatedFileId'] in file_dict:
            test_fw.write('%s\n' % json.dumps(method_info, ensure_ascii=False))
            test_file_fw.write('%s\n' % json.dumps(file_dict[method_info['affiliatedFileId']], ensure_ascii=False))

  ## calc pairs in train/test set => len(train_pair)+len(test_pair) = 1/2 len(total_pairs),
  ## since the pairs between train & test set is cut.

  # train_pairs = set()
  # test_pairs = set()
  #
  # for mid1 in train_method_id_set:
  #   for mid2 in clone_dict[mid1]:
  #     if mid2 in train_method_id_set:
  #       train_pairs.add((mid1, mid2))
  #
  # for mid1 in test_method_id_set:
  #   for mid2 in clone_dict[mid1]:
  #     if mid2 in test_method_id_set:
  #       test_pairs.add((mid1, mid2))
  # print(len(train_pairs), len(test_pairs), total_pair_cnt)


if __name__ == '__main__':
  file_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\fileInfos.txt"
  method_infos_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\methodInfos-withAPI.txt"

  file_infos = data_loader.load_method_infos(file_infos_path, None)
  method_infos = data_loader.load_method_infos(method_infos_path, None)

  random_split()
