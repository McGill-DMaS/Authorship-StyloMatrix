/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class SentenceDetectorOpenNLP extends SentenceDetector {

	private static Logger logger = LoggerFactory.getLogger(SentenceDetectorOpenNLP.class);

	private static SentenceModel model = null;

	public SentenceDetectorOpenNLP() {

		InputStream modelIn = null;
		try {
			if (model == null) {
				modelIn = new FileInputStream(NlpUtilsOpennlp.getSentenceDetectoreEnglishModelPath());
				model = new SentenceModel(modelIn);
			}
			sentenceDetector = new SentenceDetectorME(model);
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

	SentenceDetectorME sentenceDetector = null;

	@Override
	public String[] detectSentences(String rawText) {
		if (sentenceDetector != null) {
			String sentences[] = sentenceDetector.sentDetect(rawText);
			return sentences;
		} else {
			return new String[] { rawText };
		}
	}
}
