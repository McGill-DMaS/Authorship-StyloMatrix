/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;


/**
 * Enforce thread-safe tokenizer;
 * @author steven
 *
 */
public abstract class Tokenizer {
	public abstract String[] tokenize(String line);
	
	public static Tokenizer tokenizerOpenNLP = new TokenizerOpenNLP();
	public static Tokenizer newTokenizerOpenNLP(){
		return new TokenizerOpenNLP();
	}
	
	public static Tokenizer tokenizerStandford = new TokenizerStandford();
	public static Tokenizer newTokenizerStandford(){
		return new TokenizerStandford();
	}
	
	public static Tokenizer tokenizerDefault = new TokenizerDefault();
	public static Tokenizer newTokenizerDefault(){
		return new TokenizerDefault();
	}
	
	public static Tokenizer tokenizerTwitter = new TokenizerTwitter();
	public static Tokenizer newTokenizerTwitter(){
		return new TokenizerTwitter();
	}
	
	public static Tokenizer newTokenizerTwitter(String tagger){
		return new TokenizerTwitter(tagger);
	}
}
