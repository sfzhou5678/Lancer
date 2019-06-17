import json
from collections import defaultdict


def load_clone_dict(clone_pairs_path, label_propagation=None):
  clone_pairs = json.load(open(clone_pairs_path, encoding='utf-8'))
  print(len(clone_pairs))  # 79563

  clone_dict = defaultdict(dict)
  for pair in clone_pairs:
    method_id1, method_id2 = pair['methodId1'], pair['methodId2']
    clone_dict[method_id1][method_id2] = pair['cloneType']
    clone_dict[method_id2][method_id1] = pair['cloneType']

    if 'type_set' not in clone_dict[method_id1]:
      clone_dict[method_id1]['type_set'] = set()
    if 'type_set' not in clone_dict[method_id2]:
      clone_dict[method_id2]['type_set'] = set()
    clone_dict[method_id1]['type_set'].add(pair['cloneType'])
    clone_dict[method_id2]['type_set'].add(pair['cloneType'])
  print(len(clone_dict))  # 4165
  return clone_dict


def load_file_infos(fileinfo_path, line_limit):
  return load_method_infos(fileinfo_path, line_limit)


def load_method_infos(method_infos_path, line_limit=None):
  with open(method_infos_path, encoding='utf-8') as f:
    if line_limit:
      lines = [f.readline() for _ in range(line_limit)]
    else:
      lines = f.readlines()
    method_infos = [json.loads(line.strip()) for line in lines]
  return method_infos
