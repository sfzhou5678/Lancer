from elasticsearch import Elasticsearch
from tqdm import tqdm
import json


def build_index(snippets, es, snippet_index, snippet_type):
  for snippet in tqdm(snippets):
    try:
      es.index(index=snippet_index, doc_type=snippet_type, body=snippet, id=snippet['methodId'], timeout='60s')
    except:
      pass


def delete_index(snippets, es, snippet_index, snippet_type):
  for snippet in tqdm(snippets):
    try:
      es.delete(index=snippet_index, doc_type=snippet_type, body=snippet, id=snippet['methodId'], timeout='60s')
    except:
      pass


def search_snippets(query, snippet_index, snippet_type):
  search_results = es.search(index=[snippet_index], doc_type=snippet_type, body=query)
  return search_results


if __name__ == '__main__':
  es = Elasticsearch('localhost:9200')
  snippet_index = 'mix-detail-idx'
  snippet_type = 'mix-detail-type'
  method_map_path = r"D:\DeeplearningData\CloneDetection\era_bcb_sample-SLP-Detail\methodInfos-withAPI.txt"

  with open(method_map_path, encoding='utf-8') as f:
    # lines = f.readlines()[:10000]
    lines = f.readlines()
    snippets = [json.loads(line.strip()) for line in lines]

  build_index(snippets, es, snippet_index, snippet_type)
  # delete_index(snippets, es, snippet_index, snippet_type)
