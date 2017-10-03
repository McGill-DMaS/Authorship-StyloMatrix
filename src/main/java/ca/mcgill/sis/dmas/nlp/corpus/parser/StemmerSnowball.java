/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ca.mcgill.sis.dmas.env.StringResources;

public class StemmerSnowball extends Stemmer {

	SnowballStemmer stemmer= new englishStemmer();
	
	@Override
	public void stem(String[] tokens) {
		for(int i = 0; i < tokens.length; ++i){
			stemmer.setCurrent(tokens[i]);
			if(stemmer.stem()){
				tokens[i] = stemmer.getCurrent();
			}
		}
	}

}
