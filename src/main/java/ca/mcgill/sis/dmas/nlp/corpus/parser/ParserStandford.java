/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.util.Arrays;

import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class ParserStandford extends Parser {
	
	private LexicalizedParser parser = LexicalizedParser.loadModel();

	@Override
	public ParsedTree parse(String[] tokens) {
		String pennString = parser.parseStrings(Arrays.asList(tokens)).toString();
		//System.out.println(pennString);
		return ParsedTree.fromFlatString(pennString);
	}
	
	
	public static void main(String [] args){
		Sentence sentence = new Sentence("It is a great day !", Tokenizer.tokenizerStandford, Parser.parserStandford);
		System.out.print(sentence.toString());
		
	}

}
