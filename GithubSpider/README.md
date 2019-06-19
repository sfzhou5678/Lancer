## Overview

This repository provides a GitHub crawler written in **Python3**.

The crawled data will be downloaded and the corresponding meta data will be stored in local **PostgreSQL** database.



## How to use

#### 1. Init database

The file "database/create_tables.sql" records the SQL used for creating tables. After installing **PostgreSQL** , one should run these SQL first.

#### 2. Modifying configures

"crawler.py" is the main entry of the crawler. One should configure the following properties before running the crawler:

```python
base_folder = r'F:\GithubCode\Java'	
db_config = 'data/db_info.json'	# db infos, including dbName, userName, psd, host, port
seed_users = ['gaopu', 'sfzhou5678']	
target_languages = ['Java']
threads = 8
unzip = True	# whether unzip the downloaded .zip file. If Ture, the .zip file will be removed
```

#### 3. Start crawling

Run "crawler.py"and wait patiently. 