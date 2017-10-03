## PAN2014 Authorship Verification

This package contains example runs for the PAN2014 authorship verification problem. You will need to have a PAN2014 verification dataset from [here](http://pan.webis.de/clef14/pan14-web/author-identification.html). After decompressing the zip files, you should have the following directory structure:

```bash
\the_data_folder_of_your_choice
  \training
    \pan14-author-verification-training-corpus-dutch-essays-2014-04-22
    \pan14-author-verification-training-corpus-dutch-reviews-2014-04-22
    \pan14-author-verification-training-corpus-english-essays-2014-04-22
    ...
  \testing
    \pan14-author-verification-training-corpus-dutch-essays-2014-04-22
    ...
```
Make sure that the compiled jar is in your CLASSPATH environment variable. Download and unzip our bundled NLP parsing models from [here](https://github.com/McGill-DMaS/StyloMatrix/releases/download/0.0.1/nlps.7z)
The following command processes the dataset:
```bash
SET CLASSPATH=absolute_path_of_the_authorship-0.0.1-SNAPSHOT-jar-with-dependencies.jar
java ca.mcgill.sis.dmas.nlp.exp.pan2014av.PAN2014AV2 path_of_the_data_folder_of_your_choice path_of_the_unzipped_nlp_models
```
You can find the processed dataset in \the_data_folder_of_your_choice\processed\. 
Following commands will run three models for the PAN2014 dataset:
```
java -Xmx15G ca.mcgill.sis.dmas.nlp.exp.pan2014av.PAN14TestTopicLexic2Vec path_of_the_processed_folder
java -Xmx15G ca.mcgill.sis.dmas.nlp.exp.pan2014av.PAN14TestChar2Vec path_of_the_processed_folder
java -Xmx15G ca.mcgill.sis.dmas.nlp.exp.pan2014av.TestPos2Vec path_of_the_processed_folder
```
