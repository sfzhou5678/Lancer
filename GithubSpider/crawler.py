from component import CrawlerScheduler, HttpManager, DBManager, LocalFileManager, HtmlInfoProcessor, APIInfoProcessor
import os
import json
import time
import zipfile
import urllib.request
import socket
import random
import threading
from urllib.error import URLError, HTTPError
from bs4 import BeautifulSoup
import requests

if __name__ == '__main__':
  base_folder = r'F:\GithubCode\Java'
  db_config = 'data/db_info.json'
  seed_users = ['gaopu', 'sfzhou5678']
  target_languages = ['Java']
  threads = 8

  unzip = True
  clean_repo = False

  use_proxy = False
  default_timeout = 30

  db_info = json.load(open(db_config))

  # agents = [
  #   'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36']
  agents = [
    'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0',
    'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0',
    'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.137 Safari/537.36 LBBROWSER'
    'Mozilla/5.0 (Windows; U; Windows NT 5.1; it; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11',
    'Opera/9.25 (Windows NT 5.1; U; en)',
    'Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)',
    'Mozilla/5.0 (compatible; Konqueror/3.5; Linux) KHTML/3.5.5 (like Gecko) (Kubuntu)',
    'Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.12) Gecko/20070731 Ubuntu/dapper-security Firefox/1.5.0.12',
    'Lynx/2.8.5rel.1 libwww-FM/2.14 SSL-MM/1.4.1 GNUTLS/1.2.9',
    "Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.7 (KHTML, like Gecko) Ubuntu/11.04 Chromium/16.0.912.77 Chrome/16.0.912.77 Safari/535.7",
    "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:10.0) Gecko/20100101 Firefox/10.0 ",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:52.0) Gecko/20100101 Firefox/52.0"
  ]
  db = DBManager(db_info)
  http_manager = HttpManager(agents, use_proxy=use_proxy, default_timeout=default_timeout)
  file_manager = LocalFileManager(base_folder, http_manager, unzip=unzip, clean_repo=clean_repo)
  info_processor = HtmlInfoProcessor(http_manager)
  # info_processor=APIInfoProcessor(http_manager)

  scheduler = CrawlerScheduler(target_languages, seed_users,
                               db, http_manager, file_manager, info_processor,
                               threads=threads)
  scheduler.seed_warmup()
  scheduler.start()
