import os
import time
import json
import argparse
import numpy as np
from tqdm import tqdm

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import DataLoader
from torch.utils.data import Dataset

from pytorch_pretrained_bert import BertModel, BertConfig, BertForSequenceClassification, BertTokenizer
from pytorch_pretrained_bert.optimization import BertAdam

from model import BertRankingModel, BertRankingDataset, BertRankingTransform


class BertRankingManager(object):
  def __init__(self, transform, model, batch_size, device):
    self.transform = transform
    self.model = model

    self.batch_size = batch_size
    self.device = device

  def rank(self, query, candidates):
    samples = [self.transform(query, candidate, index=-1) for candidate in candidates]
    dataloader = DataLoader(samples,
                            batch_size=self.batch_size, collate_fn=self.transform.batchify, shuffle=False)

    scores = []
    for step, batch in enumerate(dataloader):
      batch = tuple(t.to(self.device) for t in batch)
      uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch = batch
      with torch.no_grad():
        logits = self.model(input_ids_batch, segment_ids_batch, input_masks_batch)
      probs = F.softmax(logits, dim=-1)
      scores.extend(probs[:, 1].detach().cpu().numpy())
    return scores


def init_bert_manager(args,base_cache_dir='ckpt/bert'):
  device = torch.device("cuda")

  # model = None
  state_save_path = args.state_save_path
  state = torch.load(state_save_path)
  model = BertForSequenceClassification.from_pretrained(args.bert_model, num_labels=2,
                                                        state_dict=state['model_state'],
                                                        cache_dir=os.path.join(base_cache_dir,'pretrained'))
  model.to(device)
  model.eval()

  print('Loaded from', state_save_path)

  tokenizer = BertTokenizer.from_pretrained(args.bert_model, do_lower_case=True, cache_dir=os.path.join(base_cache_dir,'tokenizer'))
  transform = BertRankingTransform(tokenizer=tokenizer, is_test=True, max_len=args.max_seq_length)

  bert_manager = BertRankingManager(transform, model, args.batch_size, device)

  return bert_manager


if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument("--bert_model", default='bert-base-uncased', type=str, required=False,
                      help="Bert pre-trained model selected in the list: bert-base-uncased, "
                           "bert-large-uncased, bert-base-cased, bert-base-multilingual, bert-base-chinese.")
  parser.add_argument("--state_save_path", default='ckpt/bert/rankingckpt/cls+mthName/model.state', type=str,
                      required=False)
  parser.add_argument("--max_seq_length", default=256, type=int,
                      help="The maximum total input sequence length after WordPiece tokenization. \n"
                           "Sequences longer than this will be truncated, and sequences shorter \n"
                           "than this will be padded.")
  parser.add_argument("--batch_size", default=2, type=int)
  args = parser.parse_args()

  bert_manager = init_bert_manager(args)

  query = 'input stream'
  candidates = ['output stream', 'buffer reader', 'file input reader']
  scores = bert_manager.rank(query, candidates)
  print(scores)
  # TODO: deploy a server
