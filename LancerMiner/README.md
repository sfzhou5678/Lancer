## Introduction

This repository contains the main server that interacts with **LancerPlugin**. This repository also contains the code used for training and deploying the **BERT-based deep semantic ranking model**. The Elasticsearch-based  sample retriever is stored in this project for now. 



##Main Server

The main server accepts requests from **LancerPlugin** and return the recommended code samples. The related code "server/es/remote_server.py" shows the overview workflow of our main server:

* When receive a request from plugin, this server invoke the intention analysis (language model) service to extend the given tokens. 
* Then it use the extended tokens to retrieve related code samples and remove the duplicated samples. 
* Finally, the distinct samples are (optionally) fed into BERT-based deep semantic ranking model to get the final ranked list.



## BERT-based Deep Semantic Ranking Model

* One can refer "bert_ranking_train.py" for the details of training a BERT-based model. The three main components for training BERT model **BertRankingModel**, **BertRankingTransform**, and **BertRankingDataset** locate in "model/component/ltr/BertRankingModel.py". 

* "bert_ranking_deployment.py" shows how to use the trained BERT model. 



## Sample Retriever

* **Indexing**: Code "es_index.py" shows how to build inverted indices for parsed code samples leveraging Elasticsearch (ES). 
* **Matching**: Code "retriever/es_query_builder.py" and "retriever/es_retriever.py" show how to build ES query using the given tokens & how to match candidate code samples in ES. 