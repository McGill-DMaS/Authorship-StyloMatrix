package ca.mcgill.sis.dmas.nlp.corpus.parser;

public class NlpUtilsOpennlp {
	public static volatile String PATH_NLP_MODELS;
	public static String getTaggerDutchModel(){
		return PATH_NLP_MODELS + "/nl-pos-maxent.bin";
	}
	public static String getSentenceDetectoreEnglishModelPath(){
		return PATH_NLP_MODELS + "/en-sent.bin";
	}
	public static String getTokenizerEnglishModelPath(){
		return PATH_NLP_MODELS + "/en-token.bin";
	}
	public static String getParserEnglishModelPath(){
		return PATH_NLP_MODELS + "/en-parser-chunking.bin";
	}
}
