/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;

public class SentenceDetectorStandford extends SentenceDetector {

	@Override
	public String[] detectSentences(String rawText) {
		Reader reader = new StringReader(rawText);
		DocumentPreprocessor dp = new DocumentPreprocessor(reader);

		List<String> sentenceList = new LinkedList<String>();
		Iterator<List<HasWord>> it = dp.iterator();
		while (it.hasNext()) {
		   StringBuilder sentenceSb = new StringBuilder();
		   List<HasWord> sentence = it.next();
		   for (HasWord token : sentence) {
		      if(sentenceSb.length()>1) {
		         sentenceSb.append(" ");
		      }
		      sentenceSb.append(token);
		   }
		   sentenceList.add(sentenceSb.toString());
		}
		return sentenceList.toArray(new String[sentenceList.size()]);
	}

}
