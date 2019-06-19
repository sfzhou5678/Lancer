# Overview

We implement a **method-level code parser** atop of  [SLP-Core](https://github.com/SLP-team/SLP-Core).

This repository also provides the service of **Code Intention Analysis**. 



## Code Parser

One can parse .java file to extract the key informations as follows:

 `java -jar LancerCore.jar lex-detail sourcePath destPath -l java`  

where "lex-detail" is the mode that our parser runs. The extracted methods are indexed by ES-based retriever. 



## Intention Analyzer

#### 1. Train a language model (LM)

Using the following command line to train a LSLM :

 `java -jar LancerCore.jar train-liblm --train sourcePath -m jm -o 6 -l java -e java   `  

A break-down of what happened:

- `train-liblm`: the 'mode' in which it runs. 

- `--train`: the paths (files, directories) to recursively train on

- `-m` (or `--model`): the n-gram model type to use. Use `adm` for a better natural language model, `jm` is standard (and fastest) for code

- `-o` (or `--order`): the maximum length of sequences to learn. The model complexity increases sharply with this number, and is normally no more than 5, but our tool can handle values up to about 10 on corpora of several millions of tokens, provided your RAM can too. 6 is a good pick for Java code in our experience; performance might decrease for higher values.

- `-l` (or `--language`): the language to use for the lexer. Currently we support java for code, and some simple lexers that split on e.g. punctuation and whitespace. See below for using your own.

- `-e` (or `--extension`): the extension (regex) to use for filter files in the train/test path; other (here: non-java) files are ignored. The code API also allows regexes to match against the whole filename.

  

#### 2. Deploy a trained LM

Using the following command line to deploy a trained LSLM:

 `java -jar LancerCore.jar server --lib-container containerPath -m jm -o 6 -l java -e java --cache   `  

A break-down of what happened:

- `server `: use the 'server' mode.
- `--lib-container`: the path of the libContainer obtained after training a LSLM. 
- `the remaining parameters have been introduced above, so we will not go into details.