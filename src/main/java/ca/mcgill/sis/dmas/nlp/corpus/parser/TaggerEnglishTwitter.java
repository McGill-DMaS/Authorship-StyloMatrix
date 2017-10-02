/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.StringResources;
import cmu.arktweetnlp.Tagger;

public class TaggerEnglishTwitter extends ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger {

	private static Logger logger = LoggerFactory.getLogger(TaggerEnglishTwitter.class);

	private Tagger tagger = new Tagger();

	public TaggerEnglishTwitter(String modelFile) {
		try {
			tagger.loadModel(modelFile);
		} catch (IOException e) {
			logger.error("Failed to load the tweet POS tagger.", e);
		}
	}

	@Override
	public List<String> tag(String[] tokens) {
		return tagger.tokenizeAndTag(StringResources.JOINER_TOKEN.join(Arrays.asList(tokens))).stream()
				.map(ttkn -> ttkn.tag).collect(Collectors.toList());
	}

}
