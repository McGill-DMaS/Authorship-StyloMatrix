package ca.mcgill.sis.dmas.nlp.corpus.parser;

import org.apache.commons.lang.NotImplementedException;

import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;

public class HunspellUtils {
	public static String dictionaryRoot;

	public static String getDictionary(Language lang) {
		switch (lang) {
		case english:
			return dictionaryRoot + "/en_US";
		case spanish:
			return dictionaryRoot + "/es_ANY";
		case dutch:
			return dictionaryRoot + "/nl_NL";
		case greek:
			return dictionaryRoot + "/el_GR";
		default:
			throw new NotImplementedException("not implemented spell checker for " + lang);
		}
	}
}
