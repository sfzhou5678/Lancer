from collections import Counter
from nltk.util import ngrams


def deduplicate(snippet, search_results, with_score=False):
  distinct_results = []
  if with_score:
    for res, score in search_results:
      if res['methodId'] == snippet['methodId']:
        continue
      if is_duplicated(res, distinct_results):
        continue
      distinct_results.append((res, score))

    for res, score in distinct_results:
      for key in ['text-1gram']:  # fixme
        res.pop(key)
  else:
    for res in search_results:
      if res['methodId'] == snippet['methodId']:
        continue
      if is_duplicated(res, distinct_results):
        continue
      distinct_results.append(res)

    for res in distinct_results:
      for key in ['text-1gram']:
        res.pop(key)
  return distinct_results


def is_duplicated(res, distinct_results, sim_threshold=0.9):
  for sample in distinct_results:
    if isinstance(sample, tuple):
      res2, score = sample
    else:
      res2 = sample
    sim = ngram_features('text', 'tokenSequence', res, res2, max_order=1)[0]
    if sim >= sim_threshold:
      return True
  return False


def CCLearner_jaccard_sim(freq_dict1, freq_dict2, token_df=None, decay=1.0, expand_set=None):
  key_set = set()
  for key in freq_dict1:
    key_set.add(key)
  for key in freq_dict2:
    key_set.add(key)

  sum_dis = 0
  sum_all = 0
  for key in key_set:
    if key == 'UNK' or key == 'PAD':
      continue
    freq1 = freq_dict1[key] if key in freq_dict1 else 0
    freq2 = freq_dict2[key] if key in freq_dict2 else 0

    sum_dis += abs(freq1 - freq2)
    sum_all += (freq1 + freq2)

  sim = 1.0 - sum_dis / sum_all
  return [sim]


def camel_to_underline(camel_format):
  underline_format = ''
  if isinstance(camel_format, str):
    length = len(camel_format)
    if length > 0:
      underline_format = camel_format[0]
    for i in range(1, length):
      if camel_format[i].isupper() and (not camel_format[i - 1].isupper()):
        underline_format += '_' + camel_format[i]
      else:
        underline_format += camel_format[i]

  return underline_format.lower()


def name_sim(name1, name2):
  name1 = camel_to_underline(name1)
  name2 = camel_to_underline(name2)
  words1 = name1.split('_')
  words2 = name2.split('_')

  freq_dict1 = Counter(words1)
  freq_dict2 = Counter(words2)
  sim = CCLearner_jaccard_sim(freq_dict1, freq_dict2)
  return sim


def ngram_features(base_tag, seq_key, method_info, method_info2, max_order):
  features = []
  for order in range(1, max_order + 1):
    tag = base_tag + '-%dgram' % order
    for info in [method_info, method_info2]:
      if tag not in info:
        info[tag] = Counter(ngrams(info[seq_key], order))
    features.extend(CCLearner_jaccard_sim(method_info[tag], method_info2[tag]))

  return features
