/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmu.arktweetnlp.Tagger;
import cmu.arktweetnlp.Twokenize;

public class TokenizerTwitter extends Tokenizer {

	private static Logger logger = LoggerFactory.getLogger(TaggerEnglishTwitter.class);

	private Tagger tagger = null;

	public TokenizerTwitter(String modelFile) {
		try {
			tagger = new Tagger();
			tagger.loadModel(modelFile);
		} catch (IOException e) {
			logger.error("Failed to load the tweet POS tagger.", e);
		}
	}

	public TokenizerTwitter() {
	}

	@Override
	public String[] tokenize(String str) {
		if (tagger == null) {
			List<String> ls = Twokenize.tokenizeRawTweetText(str);
			return ls.toArray(new String[ls.size()]);
		} else {
			List<String> ls = tagger.tokenizeAndTag(str).stream().map(ttkn -> ttkn.token).collect(Collectors.toList());
			return ls.toArray(new String[ls.size()]);
		}
	}

}
