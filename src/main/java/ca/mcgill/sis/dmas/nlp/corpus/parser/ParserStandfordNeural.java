/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.util.ArrayList;
import java.util.List;

import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.TypedDependency;

public class ParserStandfordNeural extends Parser {

	private DependencyParser parser = DependencyParser
			.loadFromModelFile(NlpUtilsStandford.getDependencyEnglishModelPath());
	private MaxentTagger tagger = new MaxentTagger(NlpUtilsStandford.getTaggerEnglishModelPath());

	@Override
	public ParsedTree parse(String[] tokens) {

		ArrayList<HasWord> hasWords = new ArrayList<>();
		for (String token : tokens) {
			hasWords.add(new Word(token));
		}
		List<TaggedWord> tagged = tagger.tagSentence(hasWords);
		for (TypedDependency td : parser.predict(tagged).allTypedDependencies()) {
			System.out.println(td.reln().toString());
			System.out.println(td.gov().toString());
			System.out.println(td.dep().toString());
			System.out.println();
		}

		System.out.println(parser.predict(tagged).allTypedDependencies());
		return null;
	}

	public static void main(String[] args) {
		Sentence sentence = new Sentence("It is a great day !",
				Tokenizer.tokenizerStandford, Parser.parserStandfordNeural);

		sentence = new Sentence(
				"I can almost always tell when movies use fake dinosaurs.",
				Tokenizer.tokenizerStandford, Parser.parserStandfordNeural);

		sentence = new Sentence(
				"The distinction between dependency- and constituency-based grammars derives in a large part from the initial division of the clause.",
				Tokenizer.tokenizerStandford, Parser.parserStandfordNeural);

		System.out.println(sentence.toString());
		// System.out.print(sentence.toString());

	}

}
