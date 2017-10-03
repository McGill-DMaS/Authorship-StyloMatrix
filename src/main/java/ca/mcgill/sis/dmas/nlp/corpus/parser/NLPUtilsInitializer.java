package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.LogManager;

import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsOpennlp;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsStandford;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;

public class NLPUtilsInitializer {

	public static void initialize(String nlpPackagePath, boolean silenceAll) {
		String NLP_UTIL_PATH = nlpPackagePath;
		NlpUtilsOpennlp.PATH_NLP_MODELS = NLP_UTIL_PATH + "/opennlp/";
		NlpUtilsStandford.PATH_STANDFORD_MODELS = NLP_UTIL_PATH + "/standford/";
		HunspellUtils.dictionaryRoot = nlpPackagePath + "/spellcheck/";
		global_twitter_model_path = nlpPackagePath + "/twitter/";
		if (silenceAll)
			LogManager.getLogManager().reset();
	}
	
	public static String global_twitter_model_path = "";

	public static void initialize(String nlpPackagePath) {
		initialize(nlpPackagePath, false);
	}
}
