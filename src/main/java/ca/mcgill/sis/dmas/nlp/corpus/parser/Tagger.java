/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Tagger {

	public static enum Language { //
		dutch, // opennlp (nl)
		english, // standford
		greek, //
		spanish // standford
	}

	public static Tagger getTagger(Language lang) {
		switch (lang) {
		case english:
			return new TaggerEnglishStandford();
		case greek:
			return new TaggerGreek();
		case spanish:
			return new TaggerSpanishStandford();
		case dutch:
			return new TaggerDutchOpennlp();
		default:
			return new TaggerEnglishStandford();

		}
	}

	public abstract List<String> tag(String[] tokens);

	public static Tagger taggerStanFord = new TaggerEnglishStandford();

	public static Tagger newTaggerStandFord() {
		return new TaggerEnglishStandford();
	}

	public static Tagger newTaggerStandFordSpanish() {
		return new TaggerSpanishStandford();
	}

	public static Tagger newTweetTagger(String modelFile) {
		return new TaggerEnglishTwitter(modelFile);
	}
}
