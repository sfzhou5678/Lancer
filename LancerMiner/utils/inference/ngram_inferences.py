import json
# from utils import HttpClient
from utils.http_client import HttpClient


def infer_local_tokens(snippet, tag, lm_infer, token_length, token_ratio, extend_token_len):
  """
  根据tag获取给定截断tokens和推断出来的tokens
  :param tag:
  :param lm_infer:
  :param token_length:
  :param extend_token_len:
  :return:
  """
  if token_ratio > 1:
    token_length = len(snippet[tag]) // token_ratio
  tokens = snippet[tag][: token_length]
  try:
    inferred_tokens = lm_infer.infer(snippet, token_length, extend_token_len, tag)
  except:
    # print('error', snippet['methodId'], tag)
    inferred_tokens = []
  return tokens, inferred_tokens


class LMInference(object):
  def __init__(self):
    pass

  def infer(self, snippet_info, token_length, extend_token_len, tag):
    pass


class IdealInference(LMInference):
  def __init__(self):
    super().__init__()

  def infer(self, snippet_info, token_length, extend_token_len, tag):
    return snippet_info[tag][token_length:token_length + extend_token_len]

  def __str__(self) -> str:
    return "IdealInference"


class SLPTextInference(LMInference):
  def __init__(self, host, port):
    super().__init__()
    self.http_client = HttpClient(host, port)

  def infer(self, snippet_info, token_length, extend_token_len, tag):
    # TODO: tag
    exchange_object = {
      "fileId": snippet_info['affiliatedFileId'],
      "tokenLength": token_length,
      "k": extend_token_len,
      "predTokens": []
    }
    restful_api = "predTokens"
    response = self.http_client.post(restful_api, json.dumps(exchange_object))
    predTokens = json.loads(response.text)['predTokens']
    return predTokens

  def __str__(self) -> str:
    return "SLPTextInference"
