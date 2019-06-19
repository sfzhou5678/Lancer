# Lancer

This repository contains all the code described in paper "Lancer: Your Code Tell Me What You Need".

Lancer is  a novel, context-aware, scalable, and code-to-code recommendation tool. Lancer is able to automatically analyze the intention of the incomplete code and recommend relevant and reusable code samples in real-time.

The following demo video shows what Lancer can do for you:

<iframe width="560" height="315" src="https://www.youtube.com/embed/tO9nhqZY35g" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>



## Code sructure

We implement Lancer as an IntelliJ IDEA plugin. To support this plugin, we implement a back-end server which consists of several components. These components are written in different programming language. They can run on several clusters and interact with each other via HTTP requests.

#### 1. [GithubSpider](https://github.com/sfzhou5678/Lancer/tree/master/GithubSpider)

This project provides a crawler to download the open-source code to prepare the code repository for recommendation.  The downloaded code repository can be download from [Lancer-opensource-repository.zip](https://drive.google.com/open?id=1__ylqGfBuIQ3Tth8MrXUKZOK8TyitxmO).

#### 2. [LancerParser](https://github.com/sfzhou5678/Lancer/tree/master/LancerParser)

This project provides a method-level code parser atop of  [SLP-Core](https://github.com/SLP-team/SLP-Core). This project also contains the language model we used as the Intention Analyzer introduced in our paper. One should parse the code repository using LancerParser and build a language model for predicting tokens. 

#### 3. [LancerMiner](https://github.com/sfzhou5678/Lancer/tree/master/LancerMiner)

This repository contains the main server that interacts with **LancerPlugin**. The main server shows the main workflow of Lancer's back-end system as we described in the paper. **One can go through this project first to better understand the working mechanism of Lancer.** This repository also contains the code used for training and deploying the BERT-based deep semantic ranking model. The Elasticsearch-based  sample retriever is stored in this project as well for now. 

#### 4. [LancerPlugin](https://github.com/sfzhou5678/Lancer/tree/master/LancerPlugin)

This is the implementation of the Lancer plugin on IntelliJ IDEA. The plugin can automatically send recommendation requests to the main server and show the recommend results to users.

#### 5. [LibLM Visualization Server](https://github.com/sfzhou5678/Lancer/tree/master/LibLM%20Visualization%20Server)

This project is built for visualizing the relations between libraries that we mentioned in Section II-B of the paper. 







