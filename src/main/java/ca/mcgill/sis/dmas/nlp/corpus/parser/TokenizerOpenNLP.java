/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenizerOpenNLP extends Tokenizer {

	private static Logger logger = LoggerFactory.getLogger(TokenizerOpenNLP.class);

	private TokenizerModel model;

	public TokenizerOpenNLP() {
		InputStream modelIn = null;
		try {
			if (model == null) {
				modelIn = new FileInputStream(new File(NlpUtilsOpennlp.getTokenizerEnglishModelPath()));
				model = new TokenizerModel(modelIn);
			}
			tokenizer = new TokenizerME(model);
		} catch (IOException e) {
			logger.error("Cannot load model for sentence detector.", e);
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	TokenizerME tokenizer;

	@Override
	public String[] tokenize(String line) {
		return tokenizer.tokenize(line);
	}

}
