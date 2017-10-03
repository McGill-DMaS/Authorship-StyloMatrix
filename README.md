StyloMatrix
===========

![](https://user-images.githubusercontent.com/8474647/31106051-8479ff6a-a7b6-11e7-9c29-9e4f6428521b.jpg)

Mining stylometric representation for authorship analysis. 
This repository contains several multi-language NLP utilities for text proccessing and several models for authorship analysis. The [ca.mcgill.sis.dmas.nlp.model.astyle](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/model/astyle) package contains the implementation of the following models:

* Joint Topical-Lexical Modality
* Character Modality
* Syntactic Modality
* LDA, LSA
* N-grams, static features, typed N-grams
* Two baselines from PAN2016

Example runs are included in the [ca.mcgill.sis.dmas.nlp.exp](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/exp) package. You can refer to the source code for API usage.

* [PAN2014 Authorship Verification](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/exp/pan2014av)
* [IMDB62 Authorship Identification](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/exp/imdb)
* [ICWSM2012 Authorship Characterization](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/exp/icwsm2012)
* [PAN2013 Authorship Characterization](https://github.com/McGill-DMaS/StyloMatrix/tree/master/src/main/java/ca/mcgill/sis/dmas/nlp/exp/pan2013ap)

## Compilation
This project is purely written in Java with Maven. You need the following dependencies:
* [Required] The latest x64 8.x/9.x JRE/JDK distribution from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
* [Required] The latest [Maven](https://maven.apache.org/) distribution. Its 'bin' folder should be in your system's 'Path' environment. 

The following commands will compile this project (executed at the root directory of the source code).
```bash

pushd lib/

# Install the POS tagger for Greek and its resources.
mvn install:install-file -Dfile=${basedir}/lib/GreekTagger-0.0.1.jar -DgroupId=local -DartifactId=greek-tagger -Dversion=0.0.1 -Dpackaging=jar
# Install the hunspell spell checking package.
mvn install:install-file -Dfile=${basedir}/lib/hunspell.jar -DgroupId=local -DartifactId=hunspell -Dversion=0.0.1 -Dpackaging=jar
# Install the AUROC calculation package.
mvn install:install-file -Dfile=${basedir}/lib/auc.jar -DgroupId=local -DartifactId=auc -Dversion=0.0.1 -Dpackaging=jar

popd 

# Build the final jar with all dependencies:
mvn package
# The compiled jar file target/authorship-0.0.1-SNAPSHOT-jar-with-dependencies.jar contains all the dependencies. 
# We suggest to append this jar file into your systems' 'CLASSPATH' environment variable for this session:
SET CLASSPATH=absolute_path_of_the_authorship-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Setting up the development project:
This project is written with Eclipse. You can import it as an existing eclipse maven project. Other Java IDEs that support maven projects are compatible. Please refer to the instruction of your chosen IDE to import this project. You would also need to execute the following maven commands in your IDE to resolve local dependencies:

```bash
# Install the POS tagger for Greek and its resources.
mvn install:install-file -Dfile=${basedir}/lib/GreekTagger-0.0.1.jar -DgroupId=local -DartifactId=greek-tagger -Dversion=0.0.1 -Dpackaging=jar
# Install the hunspell spell checking package.
mvn install:install-file -Dfile=${basedir}/lib/hunspell.jar -DgroupId=local -DartifactId=hunspell -Dversion=0.0.1 -Dpackaging=jar
# Install the AUROC calculation package.
mvn install:install-file -Dfile=${basedir}/lib/auc.jar -DgroupId=local -DartifactId=auc -Dversion=0.0.1 -Dpackaging=jar
```


## Licensing

The software was developed by Steven H. H. Ding under the supervision of Benjamin C. M. Fung at the McGill Data Mining and Security Lab. It is distributed under the Creative Commons Attribution-NonCommercial-NoDerivatives License. Please refer to LICENSE.txt for details.

Copyright 2017 McGill University. 
All rights reserved.
