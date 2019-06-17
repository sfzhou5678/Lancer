CREATE TABLE public.user
(
  user_id   varchar DEFAULT 128 NOT NULL,
  user_name varchar DEFAULT 128 NOT NULL,
  user_url  varchar DEFAULT 258 NOT NULL,

  primary key (user_name)
);


CREATE TABLE public.repo
(
  user_name       varchar DEFAULT 128 NOT NULL,
  repo_id         varchar DEFAULT 128 NOT NULL,
  repo_name       varchar DEFAULT 128 NOT NULL,
  repo_url        varchar DEFAULT 256 NOT NULL,
  description     varchar DEFAULT 1024,
  default_branch  varchar DEFAULT 128,
  language        varchar DEFAULT 32  NOT NULL,
  relative_save_path varchar DEFAULT 256 NOT NULL,
  create_time     timestamp,
  update_time     timestamp,
  record_time     timestamp,
  star_cnt        int     DEFAULT 0,
  fork_cnt        int     DEFAULT 0,
  file_cnt        int     DEFAULT -1,
  token_cnt       int     DEFAULT -1,
  snippet_cnt     int     DEFAULT -1,

  primary key (user_name, repo_name)
);