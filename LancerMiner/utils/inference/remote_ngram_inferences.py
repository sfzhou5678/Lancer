import json
# from utils import HttpClient
from utils.http_client import HttpClient


class RemoteLMInference(object):
  def __init__(self):
    pass

  def infer(self, code_context, snippet, extend_token_len):
    pass


class RemoteSLPTextInference(RemoteLMInference):
  def __init__(self, host, port):
    super().__init__()
    self.http_client = HttpClient(host, port)

  def infer(self, code_context_tokens, text_tokens, extend_token_len):
    try:
      exchange_object = {
        "codeContextTokens": code_context_tokens,
        "methodTokenSeq": text_tokens,
        "extendLen": extend_token_len,
        "predTokens": []
      }
      restful_api = "predTokens"
      response = self.http_client.post(restful_api, json.dumps(exchange_object))
      predTokens = response.text.strip().split()
      return predTokens
    except:
      print('error')
      return []

  def __str__(self) -> str:
    return "RemoteSLPTextInference"
