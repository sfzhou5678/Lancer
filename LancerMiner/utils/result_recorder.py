def get_clone_type(clone_dict, id1, id2):
  if id1 in clone_dict and id2 in clone_dict[id1]:
    return clone_dict[id1][id2]
  else:
    return "None-Clone"


class ResultRecoder(object):
  def __init__(self, clone_dict, k_list, clone_type_list):
    self.global_type = 'global_type'
    self.clone_dict = clone_dict
    self.k_list = k_list
    self.clone_type_list = clone_type_list

    self.init()

  def init(self):
    self.mrr_res = {}
    self.topk_hitrate = {k: {} for k in self.k_list}

  def record(self, query_method, search_results):

    global_hit = -1
    first_hit_pos = {}
    for i, method_info in enumerate(search_results):
      cur_clone_type = get_clone_type(self.clone_dict,
                                      query_method['methodId'], method_info['methodId'])
      if cur_clone_type not in first_hit_pos:
        first_hit_pos[cur_clone_type] = i
      if global_hit < 0 and cur_clone_type != "None-Clone":
        global_hit = i

    # region !... process global result
    if self.global_type not in self.mrr_res:
      self.mrr_res[self.global_type] = {'sum': 0, 'cnt': 0}
    self.mrr_res[self.global_type]['cnt'] += 1

    for k in self.k_list:
      if self.global_type not in self.topk_hitrate[k]:
        self.topk_hitrate[k][self.global_type] = {'sum': 0, 'cnt': 0}
      self.topk_hitrate[k][self.global_type]['cnt'] += 1

    if global_hit >= 0:
      global_hit += 1
      self.mrr_res[self.global_type]['sum'] += 1 / (global_hit)
      for k in self.k_list:
        if global_hit <= k:
          self.topk_hitrate[k][self.global_type]['sum'] += 1
    # endregion

    for clone_type in self.clone_dict[query_method['methodId']]['type_set']:
      if clone_type in first_hit_pos:
        hit_idx = first_hit_pos[clone_type] + 1  # 1-based
        if clone_type not in self.mrr_res:
          self.mrr_res[clone_type] = {'sum': 0, 'cnt': 0}
        self.mrr_res[clone_type]['cnt'] += 1
        self.mrr_res[clone_type]['sum'] += 1 / (hit_idx)

        for k in self.k_list:
          if clone_type not in self.topk_hitrate[k]:
            self.topk_hitrate[k][clone_type] = {'sum': 0, 'cnt': 0}
          self.topk_hitrate[k][clone_type]['cnt'] += 1
          if hit_idx <= k:
            self.topk_hitrate[k][clone_type]['sum'] += 1
      else:
        if clone_type not in self.mrr_res:
          self.mrr_res[clone_type] = {'sum': 0, 'cnt': 0}
        self.mrr_res[clone_type]['cnt'] += 1

        for k in self.k_list:
          if clone_type not in self.topk_hitrate[k]:
            self.topk_hitrate[k][clone_type] = {'sum': 0, 'cnt': 0}
          self.topk_hitrate[k][clone_type]['cnt'] += 1

  def describe(self):
    for clone_type in [self.global_type] + self.clone_type_list:
      if clone_type in self.mrr_res:
        print(clone_type, 'count:', self.mrr_res[clone_type]['cnt'])
        print(clone_type, 'MRR:', self.mrr_res[clone_type]['sum'] / self.mrr_res[clone_type]['cnt'])
      for k in self.k_list:
        if clone_type in self.topk_hitrate[k]:
          print(clone_type, 'Top %d HitRate:' % k,
                self.topk_hitrate[k][clone_type]['sum'] / self.topk_hitrate[k][clone_type]['cnt'])
      print('=' * 80)
