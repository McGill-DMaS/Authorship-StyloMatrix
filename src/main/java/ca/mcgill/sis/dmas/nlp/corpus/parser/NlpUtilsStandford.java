package ca.mcgill.sis.dmas.nlp.corpus.parser;

public class NlpUtilsStandford {
	
	public static volatile String PATH_STANDFORD_MODELS;
	
	public static String getTaggerEnglishModelPath(){
		return PATH_STANDFORD_MODELS + "/english-left3words-distsim.tagger";
	}
	
	public static String getTaggerSpanishModelPath(){
		return PATH_STANDFORD_MODELS + "/spanish-distsim.tagger";
	}
	
	public static String getDependencyEnglishModelPath(){
		return PATH_STANDFORD_MODELS + "edu/stanford/nlp/models/parser/nndep/english_UD.gz";
	}

}
