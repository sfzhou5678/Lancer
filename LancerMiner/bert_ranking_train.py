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


def warmup_linear(x, warmup=0.002):
  if x < warmup:
    return x / warmup
  return 1.0 - x


def hit_times(logits, labels):
  outputs = np.argmax(logits, axis=-1)
  return np.sum(outputs == labels)


def confusion_matrix(logits, labels):
  preds = np.argmax(logits, axis=-1)
  gt_pos_cnt = sum(labels == 1)
  gt_neg_cnt = sum(labels == 0)
  tp_cnt = sum(labels[preds == 1])
  fp_cnt = sum(1 - labels[preds == 1])
  tn_cnt = sum(1 - labels[preds == 0])
  fn_cnt = sum(labels[preds == 0])

  return gt_pos_cnt, gt_neg_cnt, tp_cnt, fp_cnt, tn_cnt, fn_cnt


def eval_running_model(dataloader):
  global eval_loss, step, batch, uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch
  model.eval()
  eval_loss, eval_hit_times = 0, 0
  nb_eval_steps, nb_eval_examples = 0, 0
  gt_pos_cnt, gt_neg_cnt, tp_cnt, fp_cnt, tn_cnt, fn_cnt = 0, 0, 0, 0, 0, 0
  for step, batch in enumerate(dataloader, start=1):
    batch = tuple(t.to(device) for t in batch)
    uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch = batch

    with torch.no_grad():
      tmp_eval_loss = model(input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch)
      logits = model(input_ids_batch, segment_ids_batch, input_masks_batch)  # TODO: speed up

    logits = logits.detach().cpu().numpy()
    label_ids = labels_batch.to('cpu').numpy()

    eval_loss += tmp_eval_loss.mean().item()
    eval_hit_times += hit_times(logits, label_ids)
    tmp_gt_pos_cnt, tmp_gt_neg_cnt, tmp_tp_cnt, tmp_fp_cnt, tmp_tn_cnt, tmp_fn_cnt = confusion_matrix(
      logits, label_ids)
    gt_pos_cnt += tmp_gt_pos_cnt
    gt_neg_cnt += tmp_gt_neg_cnt
    tp_cnt += tmp_tp_cnt
    fp_cnt += tmp_fp_cnt
    tn_cnt += tmp_tn_cnt
    fn_cnt += tmp_fn_cnt

    nb_eval_examples += labels_batch.size(0)
    nb_eval_steps += 1
  eval_loss = eval_loss / nb_eval_steps
  eval_accuracy = eval_hit_times / nb_eval_examples
  tpr = tp_cnt / gt_pos_cnt if gt_pos_cnt else 1
  fpr = fp_cnt / gt_pos_cnt if gt_pos_cnt else int(fp_cnt > 0)
  tnr = tn_cnt / gt_neg_cnt if gt_neg_cnt else 1
  fnr = fn_cnt / gt_neg_cnt if gt_neg_cnt else int(fn_cnt > 0)
  result = {
    'train_loss': tr_loss / nb_tr_steps,
    'eval_loss': eval_loss,
    'eval_accuracy': eval_accuracy,
    'tpr': tpr, 'fpr': fpr, 'tnr': tnr, 'fnr': fnr,
    'gt_pos_cnt': gt_pos_cnt, 'gt_neg_cnt': gt_neg_cnt,
    'tp_cnt': tp_cnt, 'fp_cnt': fp_cnt, 'tn_cnt': tn_cnt, 'fn_cnt': fn_cnt,

    'epoch': epoch,
    'global_step': global_step,
  }
  return result


if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  split_type = "ccl-split"
  ## Required parameters
  parser.add_argument("--bert_model", default='bert-base-uncased', type=str)
  parser.add_argument("--input_data_dir", default='data/ltr/bcb/%s/bert-full-ratio-5' % split_type, type=str)
  parser.add_argument("--output_dir", default='ckpt/bert/rankingckpt/%s/full-ratio-5' % split_type, type=str)
  parser.add_argument("--max_seq_length", default=256, type=int)
  parser.add_argument("--train_batch_size", default=16, type=int, help="Total batch size for training.")
  parser.add_argument("--eval_batch_size", default=4, type=int, help="Total batch size for eval.")
  parser.add_argument("--print_freq", default=1000, type=int, help="Total batch size for eval.")

  # parser.add_argument("--input_data_dir", default='data/ltr/bcb/%s/bert-short-ratio-5' % split_type, type=str)
  # parser.add_argument("--output_dir", default='ckpt/bert/rankingckpt/%s/short-ratio-5' % split_type, type=str)
  # parser.add_argument("--max_seq_length", default=64, type=int)
  # parser.add_argument("--train_batch_size", default=64, type=int, help="Total batch size for training.")
  # parser.add_argument("--eval_batch_size", default=32, type=int, help="Total batch size for eval.")
  # parser.add_argument("--print_freq", default=100, type=int, help="Total batch size for eval.")

  parser.add_argument("--learning_rate", default=5e-5, type=float, help="The initial learning rate for Adam.")
  parser.add_argument("--num_train_epochs", default=5.0, type=float, help="Total number of training epochs to perform.")
  parser.add_argument("--warmup_proportion", default=0.1, type=float,
                      help="Proportion of training to perform linear learning rate warmup for. E.g., 0.1 = 10%% of training.")
  # parser.add_argument("--no_cuda", default=False, action='store_true', help="Whether not to use CUDA when available")
  parser.add_argument('--seed', type=int, default=1405, help="random seed for initialization")
  parser.add_argument('--gradient_accumulation_steps', type=int, default=1,
                      help="Number of updates steps to accumulate before performing a backward/update pass.")
  parser.add_argument('--gpu', type=int, default=0)
  parser.add_argument('--eval_test', type=int, default=1)
  args = parser.parse_args()
  print(args)
  os.environ["CUDA_VISIBLE_DEVICES"] = "%d" % args.gpu

  ## init dataset and bert model
  tokenizer = BertTokenizer.from_pretrained(args.bert_model, do_lower_case=True, cache_dir='ckpt/bert/tokenizer')
  transform = BertRankingTransform(tokenizer=tokenizer, is_test=False, max_len=args.max_seq_length)

  print('=' * 80)
  print('Input dir:', args.input_data_dir)
  print('Output dir:', args.output_dir)
  print('=' * 80)
  train_file = os.path.join(args.input_data_dir, 'train-all.txt')
  val_file = os.path.join(args.input_data_dir, 'valid.txt')

  train_dataset = BertRankingDataset(train_file, transform, max_size=None)
  val_dataset = BertRankingDataset(val_file, transform, max_size=None)
  train_dataloader = DataLoader(train_dataset,
                                batch_size=args.train_batch_size, collate_fn=transform.batchify, shuffle=True)
  val_dataloader = DataLoader(val_dataset,
                              batch_size=args.eval_batch_size, collate_fn=transform.batchify, shuffle=False)
  if args.eval_test:
    test_file = os.path.join(args.input_data_dir, 'test.txt')
    test_dataset = BertRankingDataset(test_file, transform, max_size=None)
    test_dataloader = DataLoader(test_dataset,
                                 batch_size=args.eval_batch_size, collate_fn=transform.batchify, shuffle=False)

  epoch_start = 1
  global_step = 0
  best_eval_loss = 0x3f3f3f3f
  best_test_loss = 0x3f3f3f3f

  if not os.path.exists(args.output_dir):
    os.makedirs(args.output_dir)

  state_save_path = os.path.join(args.output_dir, 'model.state')
  if os.path.exists(state_save_path):
    state = torch.load(state_save_path)
    model = BertForSequenceClassification.from_pretrained(args.bert_model, num_labels=2,
                                                          state_dict=state['model_state'],
                                                          cache_dir='ckpt/bert/pretrained')
    epoch_start = state['epoch']
    global_step = state['global_step']
    best_eval_loss = state['best_eval_loss']
    state.pop('model_state')
    print('Loaded from', state_save_path)
    for key in state:
      if key not in ['model_state', 'opt_state']:
        print(key, state[key])
  else:
    model = BertForSequenceClassification.from_pretrained(args.bert_model, num_labels=2,
                                                          cache_dir='ckpt/bert/pretrained')
  model = model.cuda()

  param_optimizer = list(model.named_parameters())
  param_optimizer = [n for n in param_optimizer if 'pooler' not in n[0]]
  no_decay = ['bias', 'LayerNorm.bias', 'LayerNorm.weight']
  optimizer_grouped_parameters = [
    {'params': [p for n, p in param_optimizer if not any(nd in n for nd in no_decay)], 'weight_decay': 0.01},
    {'params': [p for n, p in param_optimizer if any(nd in n for nd in no_decay)], 'weight_decay': 0.0}
  ]
  optimizer = BertAdam(optimizer_grouped_parameters, lr=args.learning_rate)

  if os.path.exists(state_save_path):
    optimizer.load_state_dict(state['opt_state'])

  device = torch.device("cuda")

  tr_total = int(
    train_dataset.__len__() / args.train_batch_size / args.gradient_accumulation_steps * args.num_train_epochs)
  print_freq = args.print_freq
  eval_freq = len(train_dataloader) // 4
  print('Print freq:', print_freq, "Eval freq:", eval_freq)

  for epoch in range(epoch_start, int(args.num_train_epochs) + 1):
    tr_loss = 0
    nb_tr_examples, nb_tr_steps = 0, 0
    with tqdm(total=len(train_dataloader)) as bar:
      for step, batch in enumerate(train_dataloader, start=1):
        model.train()
        optimizer.zero_grad()
        batch = tuple(t.to(device) for t in batch)
        uuid_batch, input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch = batch
        loss = model(input_ids_batch, segment_ids_batch, input_masks_batch, labels_batch)
        loss.backward()

        tr_loss += loss.item()
        nb_tr_examples += input_ids_batch.size(0)
        nb_tr_steps += 1

        lr_this_step = args.learning_rate * warmup_linear(global_step / tr_total, args.warmup_proportion)
        for param_group in optimizer.param_groups:
          param_group['lr'] = lr_this_step
        optimizer.step()
        optimizer.zero_grad()
        global_step += 1

        if step % print_freq == 0:
          bar.update(min(print_freq, step))
          time.sleep(0.02)
          print(global_step, tr_loss / nb_tr_steps)

        if global_step % eval_freq == 0:
          val_result = eval_running_model(val_dataloader)

          if val_result['eval_loss'] < best_eval_loss:
            best_eval_loss = val_result['eval_loss']
            val_result['best_eval_loss'] = best_eval_loss
            print('Global Step %d VAL res:\n' % global_step, val_result)
            # save model
            print('[Saving at]', state_save_path)

            val_result['model_state'] = model.state_dict()
            val_result['opt_state'] = optimizer.state_dict()
            torch.save(val_result, state_save_path)

    if args.eval_test:
      test_result = eval_running_model(test_dataloader)
      print('Global Step %d TEST res:\n' % global_step, test_result)
      if test_result['eval_loss'] < best_test_loss:
        best_test_loss = test_result['eval_loss']
        val_result['best_test_loss'] = best_test_loss

        val_result['model_state'] = model.state_dict()
        val_result['opt_state'] = optimizer.state_dict()
        torch.save(val_result, state_save_path + ".test")
