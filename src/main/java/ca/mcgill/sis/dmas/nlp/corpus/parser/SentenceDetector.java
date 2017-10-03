/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

public abstract class SentenceDetector {
	public abstract String [] detectSentences(String rawText);
	
	public static SentenceDetector sentenceDetectorOpenNLP = new SentenceDetectorOpenNLP();
	public static SentenceDetector newSentenceDetectorOpenNLP(){
		return new SentenceDetectorOpenNLP();
	}
	
	public static SentenceDetector sentenceDetectorStandford = new SentenceDetectorStandford();
	public static SentenceDetector newSentenceDetectorStanford(){
		return new SentenceDetectorStandford();
	}
	
	
}
