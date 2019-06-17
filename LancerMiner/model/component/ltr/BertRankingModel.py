import json
import time
import numpy as np
from tqdm import tqdm

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import DataLoader, Dataset

from pytorch_pretrained_bert.modeling import BertPreTrainedModel
from pytorch_pretrained_bert import BertModel
from pytorch_pretrained_bert import BertTokenizer, BasicTokenizer

from utils import ParserUtil


class BertRankingModel(BertPreTrainedModel):
  def __init__(self, config):
    super(BertRankingModel, self).__init__(config)
    self.bert = BertModel(config)

    # self.out_layer = OuterNet(
    #   x_size=768,  # bert的输出shape
    #   y_size=0,
    #   hidden_size=128,
    #   dropout_rate=0.1,
    # )
    # self.criterion = RougeLoss().cuda()
    self.apply(self.init_bert_weights)

  def forward(self, input_ids, token_type_ids=None, attention_mask=None, labels=None):
    sequence_output, _ = self.bert(input_ids, token_type_ids, attention_mask, output_all_encoded_layers=False)
    out = self.out_layer(sequence_output, sequence_output, attention_mask, attention_mask)

    if labels is not None:
      return self.criterion(out, labels), out
    else:
      return out


class BertRankingTransform(object):
  def __init__(self, tokenizer, is_test, max_len=512):
    self.tokenizer = tokenizer
    self.max_len = max_len
    self.is_test = is_test

  def __call__(self, text_a, text_b, index, label=None):
    uuid = index

    tokens_a = self.tokenizer.tokenize(text_a)
    tokens_b = self.tokenizer.tokenize(text_b)
    tokens_a = tokens_a[:self.max_len - 3]
    tokens_b = tokens_b[:max(0, self.max_len - 3 - len(tokens_a))]

    tokens = ["[CLS]"] + tokens_a + ["[SEP]"]
    segment_ids = [0] * len(tokens)
    tokens += tokens_b + ["[SEP]"]
    segment_ids += [1] * (len(tokens_b) + 1)

    input_ids = self.tokenizer.convert_tokens_to_ids(tokens)
    input_masks = [1] * len(input_ids)
    padding = [0] * (self.max_len - len(input_ids))
    input_ids += padding
    input_masks += padding
    segment_ids += padding

    assert len(input_ids) == self.max_len
    assert len(input_masks) == self.max_len
    assert len(segment_ids) == self.max_len

    if self.is_test:
      return uuid, input_ids, segment_ids, input_masks
    else:
      return uuid, input_ids, segment_ids, input_masks, label

  def batchify(self, batch):
    uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch = [], [], [], []
    if not self.is_test:
      labels_batch = []
    for sample in batch:
      uuid, input_ids, segment_ids, input_masks = sample[:4]

      uuid_batch.append(uuid)
      input_ids_batch.append(input_ids)
      segment_ids_batch.append(segment_ids)
      input_masks_batch.append(input_masks)
      if not self.is_test:
        labels_batch.append(sample[-1])

    long_tensors = [uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch]
    uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch = (
      torch.tensor(t, dtype=torch.long) for t in long_tensors)
    if self.is_test:
      return uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch
    else:
      labels_batch = torch.tensor(labels_batch, dtype=torch.long)
      return uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch


class BertRankingDataset(Dataset):
  def __init__(self, data_path, transform, max_size=None):
    data_source = []
    with open(data_path, encoding='utf-8') as f:
      lines = f.readlines()
      if max_size is not None and max_size > 0:
        lines = lines[:max_size]
      for line in lines:
        data_source.append(json.loads(line))

    self.data_source = data_source
    self.transformed_data = {}
    self.transform = transform

  def __len__(self):
    return len(self.data_source)

  def __getitem__(self, indices):
    if isinstance(indices, (tuple, list)):
      return [self.__get_single_item__(index) for index in indices]
    return self.__get_single_item__(indices)

  def __get_single_item__(self, index):
    if index in self.transformed_data:
      key_data = self.transformed_data[index]
      return key_data
    else:
      text_a = self.data_source[index]['textA']
      text_b = self.data_source[index]['textB']
      label = int(int(self.data_source[index]['label']) >= 1)
      key_data = self.transform(text_a, text_b, index, label)
      self.transformed_data[index] = key_data

      return key_data
