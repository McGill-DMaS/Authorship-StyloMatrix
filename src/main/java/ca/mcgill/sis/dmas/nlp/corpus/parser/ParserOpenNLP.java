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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.StringResources;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

public class ParserOpenNLP extends Parser {

	private static Logger logger = LoggerFactory.getLogger(ParserOpenNLP.class);

	private ParserModel model;
	opennlp.tools.parser.Parser parser;

	public ParserOpenNLP() {
		InputStream modelIn = null;
		if (model == null) {
			try {
				modelIn = new FileInputStream(new File(NlpUtilsOpennlp.getParserEnglishModelPath()));
				model = new ParserModel(modelIn);
			} catch (IOException e) {
				logger.error("Failed to load opennlp parser model..", e);
				e.printStackTrace();
			} finally {
				if (modelIn != null) {
					try {
						modelIn.close();
					} catch (IOException e) {
					}
				}
			}
		}
		parser = ParserFactory.create(model);
	}

	@Override
	public ParsedTree parse(String[] tokens) {
		Parse[] parses = ParserTool.parseLine(StringResources.JOINER_TOKEN.join(tokens), parser, 1);
		StringBuffer sBuffer = new StringBuffer();
		parses[0].show(sBuffer);
		return ParsedTree.fromFlatString(sBuffer.toString());
	}

	public static void main(String[] args) {
		Parser parser = new ParserOpenNLP();
		ParsedTree pt = parser.parse(new String[] { "it", "is", "a", "great", "great", "day", "!" });
		System.out.println(pt.toFlatString());
	}

}
