import urllib.request
import socket
import random
import time
from urllib.error import URLError, HTTPError


class HttpManager(object):
  def __init__(self, agents, use_proxy=False, default_timeout=0, sleep_gap=1):
    self.agents = agents
    self.use_proxy = use_proxy
    self.default_timeout = default_timeout
    self.sleep_gap = sleep_gap
    if self.default_timeout:
      socket.setdefaulttimeout(self.default_timeout)

  def download_connect(self, url):
    time.sleep(self.sleep_gap)
    connect = urllib.request.urlopen(url)
    return connect

  def read_url(self, url):
    time.sleep(self.sleep_gap)
    if not self.use_proxy:
      req = urllib.request.Request(url)
      req.add_header('User-Agent', self.agents[random.randint(0, len(self.agents) - 1)])
      page_source = urllib.request.urlopen(req).read()
      page_source = page_source.decode("UTF-8")
    else:
      # TODO proxies
      # if len(self.proxies) <= 10:
      #   self.update_proxies()
      # page_source = '[]'
      # while len(proxies) > 0:
      #   header = {'User-Agent': agents[random.randint(0, len(agents) - 1)]}
      #   proxy = get_random_proxy()
      #   proxy_handler = urllib.request.ProxyHandler(proxy)
      #   opener = urllib.request.build_opener(proxy_handler)
      #   request = urllib.request.Request(url, headers=header)
      #   try:
      #     page_source = opener.open(request).read().decode('utf-8')
      #     break
      #   except HTTPError as e:
      #     print('HTTP Error, error code: ', e.code)
      #     proxies.remove(proxy['http'])
      #     overdue_ips.add(proxy['http'])
      #   except URLError as e:
      #     print('Url error, Reason: ', e.reason)
      #     return '[]'
      pass

    return page_source

  def update_proxies(self):
    print('Update proxies...')

    for page in range(3):
      url = 'http://www.xicidaili.com/nn/%s' % ('' if page == 0 else str(page + 1))

      headers = {'User-Agent': agents[random.randint(0, len(agents) - 1)]}
      web_data = requests.get(url, headers=headers)
      soup = BeautifulSoup(web_data.text, 'lxml')
      ips = soup.find_all('tr')
      for i in range(1, len(ips)):
        ip_info = ips[i]
        tds = ip_info.find_all('td')
        ip = 'http://' + tds[1].text + ':' + tds[2].text
        if ip not in overdue_ips and ip not in proxies:
          proxies.append(ip)
    print('Available proxies num:', len(proxies))
