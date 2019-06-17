class BasicQueryBuilder(object):
  def __init__(self, fields):
    self.fields = fields

  def build_query(self, tokens, max_size):
    query = {
      'query': {
        'multi_match': {
          'query': ' '.join(tokens),
          'type': 'cross_fields',
          'fields': self.fields
        }
      },
      'size': max_size
    }
    return query


class ExtendQueryBuilder(object):
  def __init__(self, fields):
    self.fields = fields

  def build_query(self, tokens, inferred_tokens, max_size):
    raise NotImplementedError()


class SimpleExtendQueryBuilder(ExtendQueryBuilder):
  def build_query(self, tokens, inferred_tokens, max_size):
    query = {
      'query': {
        'multi_match': {
          'query': ' '.join(tokens + inferred_tokens),
          'type': 'cross_fields',
          'fields': self.fields
        }
      },
      'size': max_size
    }
    return query


class BoolExtendQueryBuilder(ExtendQueryBuilder):
  def build_query(self, tokens, inferred_tokens, max_size):
    raw_query = {
      'multi_match': {
        'query': ' '.join(tokens),
        'type': 'cross_fields',
        'fields': self.fields
      }}
    extend_query = {
      'multi_match': {
        'query': ' '.join(inferred_tokens),
        'type': 'cross_fields',
        'fields': self.fields
      }}

    query = {
      "query": {
        "bool": {
          "should": [raw_query, extend_query]
        }
      },
      "size": max_size
    }
    return query


class CombineQueryBuilder(object):
  def __init__(self, text_fields, api_fields):
    self.text_fields = text_fields
    self.api_fields = api_fields

  def build_query(self, text_tokens, api_tokens, max_size):
    text_query = {
      'multi_match': {
        'query': ' '.join(text_tokens),
        'type': 'cross_fields',
        'fields': self.text_fields
      }}
    api_query = {
      'multi_match': {
        'query': ' '.join(api_tokens),
        'type': 'cross_fields',
        'fields': self.api_fields
      }}

    query = {
      "query": {
        "bool": {
          "should": [text_query, api_query]
        }
      },
      "size": max_size
    }
    return query
