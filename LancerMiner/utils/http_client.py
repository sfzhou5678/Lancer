import requests


class HttpClient(object):
  def __init__(self, host, port):
    self.host = host
    self.port = port
    self.default_headers = {'Content-Type': 'application/json'}

  def post(self, restful_api, data, headers=None):
    url = "http://%s:%d/%s" % (self.host, self.port, restful_api)
    response = requests.post(url, data=data, headers=self.default_headers if not headers else headers)
    return response


if __name__ == '__main__':
  import json

  # demo
  host = "127.0.0.1"
  port = 41235
  http_client = HttpClient(host, port)

  restful_api = "predTokens"
  # data = {
  #   "fileId": "1234",
  #   "tokenLength": 100,
  #   "k": 10,
  # }
  data = {'fileId': '19d9c98dc0f9a532f15e6687f7d4ec66', 'tokenLength': 7, 'k': 5, 'predTokens': []}
  # data = {
  #   "fileId": "25c9a8758577e601629b1656b918d6fc",
  #   "tokenLength": 120,
  #   "k": 10,
  # }

  for i in range(10000):
    try:
      response = http_client.post(restful_api, json.dumps(data))
      print(i, json.loads(response.text))
    except:
      print(i, 'error')
