/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser.ParsedTree;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public class Sentence implements Iterable<String> {

	public String[] tokens;
	public ParsedTree tree = null;

	public Sentence(String line) {
		line = line.trim();
		if (line.startsWith("(") && line.endsWith(")")) {
			tree = ParsedTree.fromFlatString(line);
			tokens = tree.tokens;
		} else {
			tokens = line.split(StringResources.STR_TOKENBREAK);
		}
	}

	public Sentence(String line, Tokenizer tokenizer) {
		if (tokenizer == null) {
			tokens = line.split(StringResources.STR_TOKENBREAK);
		} else {
			tokens = tokenizer.tokenize(line);
		}
	}

	public Sentence(String line, Tokenizer tokenizer, Parser parser) {
		if (tokenizer == null) {
			tokens = line.split(StringResources.STR_TOKENBREAK);
		} else {
			tokens = tokenizer.tokenize(line);
		}
		if (parser != null) {
			tree = parser.parse(tokens);
		}
	}

	public void parse(Parser parser) {
		if (parser != null) {
			tree = null;
			tree = parser.parse(tokens);
		}
	}

	public Sentence() {
	}

	public String toString() {
		if (tree != null)
			return tree.toFlatString();
		else
			return StringResources.JOINER_TOKEN.join(tokens);
	}

	public static Sentence[] convert(ArrayList<String> strs) {
		ArrayList<Sentence> sentences = new ArrayList<>();
		for (String str : strs) {
			sentences.add(new Sentence(str));
		}
		return sentences.toArray(new Sentence[sentences.size()]);
	}

	@Override
	public Iterator<String> iterator() {
		return Arrays.asList(tokens).iterator();
	}

}
