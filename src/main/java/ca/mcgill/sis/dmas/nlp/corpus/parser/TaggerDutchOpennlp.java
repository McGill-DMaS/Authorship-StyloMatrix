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
import java.util.Arrays;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaggerDutchOpennlp extends Tagger {

	private static Logger logger = LoggerFactory
			.getLogger(TaggerDutchOpennlp.class);

	private POSModel model;

	public TaggerDutchOpennlp() {
		InputStream modelIn = null;
		try {
			if (model == null) {
				modelIn = new FileInputStream(new File(NlpUtilsOpennlp.getTaggerDutchModel()));
				model = new POSModel(modelIn);
			}
			tagger = new POSTaggerME(model);
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

	POSTaggerME tagger;

	@Override
	public List<String> tag(String[] tokens) {
		return Arrays.asList(tagger.tag(tokens));
	}

}
