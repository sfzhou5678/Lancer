from bs4 import BeautifulSoup
import re
from datetime import datetime


class InfoProcessor(object):
  def __init__(self):
    pass

  def get_user_info(self, user_name):
    pass

  def get_repo_infos(self, user_name, type):
    """
    获取跟指定的user相关的repo, 可能是他所star的，也可能是她自己所拥有的repos
    :param user_name:
    :param type: ['starred', 'repos']
    :return:
    """
    pass

  def get_stargazers(self, stargazers_url):
    pass


class HtmlInfoProcessor(InfoProcessor):
  def __init__(self, http_manager):
    super().__init__()
    self.type_map = {'starred': 'stars',
                     'repos': 'repositories'}
    self.http_manager = http_manager

  def get_user_info(self, user_name):
    """
    由于无法直接从html上获取user_id, 所以先记录为空值，等待后序调用api获取id
    :param user_name:
    :return:
    """
    user_info = {
      'user_id': '',
      'user_name': user_name,
      'user_url': 'https://github.com/%s' % user_name}
    return user_info

  def get_repo_infos(self, user_name, type):
    """
    获取跟指定的user相关的repo, 可能是他所star的，也可能是她自己所拥有的repos
    形如: https://github.com/sfzhou5678?tab=stars
    :param user_name:
    :param type: ['stars', 'repositories']
    :return:
    """
    type = self.type_map[type]
    url = 'https://github.com/%s?tab=%s' % (user_name, type)
    ts_now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    repo_infos = []
    page_cnt = 0
    page_threshold = 2
    while url:
      page_source = self.http_manager.read_url(url)
      bsobj = BeautifulSoup(page_source, 'lxml')

      if type == self.type_map['starred']:
        project_list = bsobj.find_all('div', {'class': 'col-12 d-block width-full py-4 border-bottom'})
      else:
        # 不会选中fork的项目
        project_list = bsobj.find_all('li', {'class': 'col-12 d-flex width-full py-4 border-bottom public source'})

      for j in range(len(project_list)):
        project = project_list[j]

        repo_short_url = project.find(
          'div', {'class': 'd-inline-block mb-1'}).find('a')['href'][1:]  # 得到形如'mJackie/RecSys'
        owner_name, repo_name = repo_short_url.split('/')
        repo_url = 'https://github.com/%s' % repo_short_url

        try:
          # <span itemprop="programmingLanguage">Java</span>
          # repo_language = project.find('div', {'class': 'f6 text-gray mt-2'}).find('span',
          #                                                                          {'class': 'mr-3'}).string.strip()
          repo_language = project.find('div', {'class': 'f6 text-gray mt-2'}).find('span',
                                                                                   {
                                                                                     'itemprop': 'programmingLanguage'}).string.strip()
        except:
          repo_language = ''

        try:
          description = project.find('p', {'itemprop': "description"}).text.strip()
        except:
          description = ''
        star_cnt, fork_cnt = 0, 0
        cnt_info = project.find('div', {'class': 'f6 text-gray mt-2'}).find_all('a', {'class': 'muted-link mr-3'})
        for info in cnt_info:
          # 如果star=0, 则页面上不会有相应的数字显示，这个for可以避免这种情况引起的bug
          cnt = int(self.get_count(info.text))
          href = info['href']
          cnt_type = href.split('/')[-1]
          if cnt_type == 'stargazers':
            star_cnt = cnt
          else:
            fork_cnt = cnt

        repo_info = {'user_name': owner_name,
                     'repo_id': '',
                     'repo_name': repo_name,
                     'repo_url': repo_url,
                     'language': repo_language,
                     'default_branch': 'master',
                     'description': description,
                     'create_time': None,
                     'update_time': None,
                     'record_time': ts_now,

                     'star_cnt': star_cnt,
                     'fork_cnt': fork_cnt,

                     'stargazers_url': repo_url + '/stargazers'
                     }
        repo_infos.append(repo_info)
      page_cnt += 1
      if page_cnt >= page_threshold:
        break
      url = self.get_next_page_url(bsobj)
    return repo_infos

  def get_count(self, string):
    nums = re.findall('[0-9]+', string)
    return nums[-1]

  def get_next_page_url(self, page_source):
    paginate_container = page_source.find('div', {'class': 'paginate-container'})
    if not paginate_container:
      return None
    next_btn = paginate_container.find_all('a', {'class': 'btn btn-outline BtnGroup-item'})[-1]

    if next_btn.text == 'Next':
      url = next_btn['href']
    else:
      url = None

    return url

  def get_stargazers(self, url):
    star_gazers = []
    cnt = 0
    threshold = 1
    while url:
      page_source = self.http_manager.read_url(url)
      page_source = BeautifulSoup(page_source, 'lxml')
      gazer_list = page_source.find_all('li', {'class': 'follow-list-item float-left border-bottom'})
      for i in range(len(gazer_list)):
        gazer = gazer_list[i]
        user_name = gazer.find('h3', {'class': 'follow-list-name'}).find('a')['href'][1:]  # 得到的是/zsf5678, 用[1:]去掉'/'
        user_info = self.get_user_info(user_name)
        star_gazers.append(user_info)
      cnt += 1
      if cnt >= threshold:
        break
      url = self.get_next_page_url(page_source)

    return star_gazers


class APIInfoProcessor(InfoProcessor):
  def __init__(self):
    super().__init__()

  def get_user_info(self, user_name):
    """
    调用http读取user info, 如果读取失败就只保存user_name
    :param user_name:
    :return:
    """
    user_info = {'user_name': user_name}

    # TODO:
    # url = ''
    # try:
    #   data = self.http_manager.read_url(url)
    #   data = json.loads(data)
    # except:
    #   pass

    return user_info

  def get_repo_infos(self, user_name, type):
    """
    获取跟指定的user相关的repo, 可能是他所star的，也可能是她自己所拥有的repos
    type=['repos','starred']
    1. 获取某个user的repos信息: https://api.github.com/users/{user_name}/repos
    2. 获取某用户所有star了的repo的信息(和2相反):https://api.github.com/users/{user_name}/starred
    :param user_name:
    :param type:
    :return:
    """
    url = 'https://api.github.com/users/%s/%s' % (user_name, type)
    repos = json.loads(self.http_manager.read_url(url))
    # try:
    #   repos = json.loads(read_url(url))
    # except:
    #   # TODO: 记录错误记录
    #   repos = []
    #   print('Get repos error_%s_%s' % (user_name, type))
    #   time.sleep(3)

    # user_name=repo_info['owner']['login']
    # repo_id = repo_info['repo_id']
    # repo_url = repo_info['repo_url']
    # default_branch = repo_info['default_branch']
    # description = repo_info['description']
    # if description is None:
    #   description = ''
    #
    # create_time = transform_datetime(repo_info['created_at'])
    # update_time = transform_datetime(repo_info['updated_at'])
    #
    # ## 因为要做personal, 所以就不舍star的阈值了, 不过之后可以在db里根据star数进行筛选
    # star_cnt = repo_info['stargazers_count']
    # fork_cnt = repo_info['forks_count']
    #
    return repos

  def get_stargazers(self, stargazers_url):
    # FIXME:
    try:
      users = json.loads(self.http_manager.read_url(stargazers_url))
    except:
      users = []
    return users
