import os
import json


def traverse(folder, file_info_wf, method_info_wf):
  """
  遍历文件夹，提取file info和method info到一个文件中
  :param path:
  :param file_info_wf:
  :param method_info_wf:
  :return:
  """
  for file in os.listdir(folder):
    cur_path = os.path.join(folder, file)
    if os.path.isdir(cur_path):
      traverse(cur_path, file_info_wf, method_info_wf)
    else:
      try:
        with open(cur_path, encoding='utf-8') as f:
          data = json.load(f)
          file_info_wf.write('%s\n' % json.dumps(data['fileInfo'], ensure_ascii=False))
          file_info_wf.flush()
          for method_info in data['methodInfoList']:
            method_info_wf.write('%s\n' % json.dumps(method_info, ensure_ascii=False))
          method_info_wf.flush()
      except:
        pass


base_folder = r'D:\DeeplearningData\CloneDetection'
file_info_wf = open(os.path.join(base_folder, 'fileInfos.txt'), 'w', encoding='utf-8')
method_info_wf = open(os.path.join(base_folder, 'methodInfos.txt'), 'w', encoding='utf-8')

traverse(base_folder, file_info_wf, method_info_wf)

file_info_wf.close()
method_info_wf.close()
