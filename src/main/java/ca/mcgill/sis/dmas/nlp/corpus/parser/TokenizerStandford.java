/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.StringReader;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;

public class TokenizerStandford extends Tokenizer {

	@Override
	public String[] tokenize(String line) {
		StringReader sr = new StringReader(line);
		PTBTokenizer<Word> tkzr = PTBTokenizer.newPTBTokenizer(sr);
		List<Word> toks = tkzr.tokenize();
		String [] tokens = new String [toks.size()];
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = toks.get(i).word().toString();
		}
		return tokens;
	}

}
