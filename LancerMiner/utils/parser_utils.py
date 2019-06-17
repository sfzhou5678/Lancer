def camel_to_underline(camel_format):
  underline_format = ''
  if isinstance(camel_format, str):
    length = len(camel_format)
    if length > 0:
      underline_format = camel_format[0]
    for i in range(1, length):
      if camel_format[i].isupper() and (not camel_format[i - 1].isupper()):
        underline_format += '_' + camel_format[i]
      else:
        underline_format += camel_format[i]

  return underline_format.lower()


def is_str(token):
  return token.startswith("\"")


class ParserUtil(object):
  reversed_words = ["abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
                    "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
                    "long", "native", "new", "package", "private", "protected", "public", "return", "short",
                    "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                    "transient", "try", "void", "volatile", "while", "true", "false", "null", "main",
                    "(", ")", "{", "}", ".", ";", "@", "+", "[", "]", "!", ",", ":",
                    "=", "==", "<", ">", "!=", ">=", "<=",
                    ]
  REVERSED_SET = set(reversed_words)

  @staticmethod
  def extractNLwords(tokens):
    keywords = []
    for token in tokens:
      token = token.strip()
      s = ""
      s.isnumeric()
      if token == "" or is_str(token) or token.isdecimal():
        continue

      if token not in ParserUtil.REVERSED_SET:
        underline_format_token = camel_to_underline(token)
        for word in underline_format_token.split("_"):
          if word.isalpha():
            keywords.append(word)
    return keywords
