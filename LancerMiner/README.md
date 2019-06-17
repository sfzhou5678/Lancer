## Introduction

This repository contains the main server that interacts with **LancerPlugin**. This repository also contains the code used for training and deploying the **BERT-based deep semantic ranking model**.



#### Main Server

The main server accepts requests from **LancerPlugin** and return the recommended code samples. The related code "server/es/remote_server.py" shows the overview workflow of our main server.



#### BERT-based deep semantic ranking model

* One can refer "bert_ranking_train.py" for the details of training a BERT-based model. The three main components for training BERT model **BertRankingModel**, **BertRankingTransform**, and **BertRankingDataset** locate in "model/component/ltr/BertRankingModel.py". 

* "bert_ranking_deployment.py" shows how to use the trained BERT model. 

