/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import ca.mcgill.sis.dmas.env.StringResources;

public abstract class Stemmer {
	public abstract void stem(String [] tokens);
	
	
	public String stem(String line, Tokenizer tokenizer){
		String [] tokens = tokenizer.tokenize(line);
		stem(tokens);
		StringBuilder sBuilder = new StringBuilder();
		for (String string : tokens) {
			sBuilder.append(string).append(StringResources.STR_TOKENBREAK);
		}
		return sBuilder.toString();
	}
	
	
	public String stem(String line){
		return stem(line, Tokenizer.tokenizerDefault);
	}
	
	
	
	public static Stemmer getStemmerSnowball(){
		return new StemmerSnowball();
	}
	
}
