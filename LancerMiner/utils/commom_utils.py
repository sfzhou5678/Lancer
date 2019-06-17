import pickle
import numpy as np


def pickle_dump(data, file_path):
  f_write = open(file_path, 'wb')
  pickle.dump(data, f_write, True)


def pickle_load(file_path):
  f_read = open(file_path, 'rb')
  data = pickle.load(f_read)

  return data


def dict_add(dict, key, step=1):
  if key not in dict:
    dict[key] = 0
  dict[key] += step


def reverse_dict(cur_dict):
  new_dict = {}
  for key, value in cur_dict.items():
    new_dict[value] = key
  return new_dict


def lookup_vocab(vocab, word, unk_id=1):
  if word in vocab:
    return vocab[word]
  else:
    return unk_id


def softmax(x):
  x -= x.max()
  return np.exp(x) / sum(np.exp(x))


if __name__ == '__main__':
  test_data = np.array([0, 1,2])
  print(softmax(test_data))
