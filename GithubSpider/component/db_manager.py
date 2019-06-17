import psycopg2


class DBManager(object):
  def __init__(self, db_info):
    self.conn = psycopg2.connect(database=db_info['database'], user=db_info['user'], password=db_info['password'],
                                 host=db_info['host'], port=db_info['port'])
    self.cur = self.conn.cursor()  # 创建指针对象

  def commit(self):
    self.conn.commit()

  def record_user(self, user_info):
    try:
      self.cur.execute('INSERT INTO "user"(user_id, user_name, user_url) VALUES(%s,%s,%s)',
                       (user_info['user_id'], user_info['user_name'], user_info['user_url']))
    except Exception as e:
      print(e)

  def record_repo(self, repo_info):
    try:
      self.cur.execute(
        'INSERT INTO repo(user_name, repo_id, repo_name, repo_url, description, default_branch, language, relative_save_path,'
        'create_time,update_time,record_time,star_cnt,fork_cnt) '
        'VALUES( %s, %s, %s, %s, %s, %s, %s, %s, %s,%s,%s,%s,%s)',
        (repo_info['user_name'], repo_info['repo_id'], repo_info['repo_name'], repo_info['repo_url'],
         repo_info['description'], repo_info['default_branch'], repo_info['language'], repo_info['relative_save_path'],
         repo_info['create_time'], repo_info['update_time'], repo_info['record_time'],
         repo_info['star_cnt'], repo_info['fork_cnt']))
    except Exception as e:
      print(e)
