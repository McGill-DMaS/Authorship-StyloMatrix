/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.StringResources;
import edu.stanford.nlp.process.TokenizerAdapter;
import edu.stanford.nlp.trees.PennTreebankTokenizer;

public abstract class Parser {

	private static Logger logger = LoggerFactory.getLogger(Parser.class);

	public abstract ParsedTree parse(String[] tokens);

	public ParsedTree parse(String line, Tokenizer tokenizer) {
		return parse(tokenizer.tokenize(line));
	}

	public static Parser parserStandford = new ParserStandford();
	public static Parser parserStandfordNeural = new ParserStandfordNeural();
	public static Parser parserOpenNLP = new ParserOpenNLP();

	public static Parser newParserStandford() {
		return new ParserStandford();
	}
	public static Parser newParserStandfordNeural() {
		return new ParserStandfordNeural();
	}

	public static Parser newParserOpenNLP() {
		return new ParserOpenNLP();
	}

	public static class ParsedTree {
		public SubTree root;
		public ArrayList<SubTree> leaves = new ArrayList<>();
		public String[] tokens;
		
		public String toFlatString() {
			StringWriter sw = new StringWriter();
			root.writeChildren(root, sw);
			return sw.toString();
		}
		
		public static ParsedTree fromFlatString(String pennString) {
			PennTreebankTokenizer tokenizer = new PennTreebankTokenizer(
					new StringReader(pennString));
			if (!tokenizer.hasNext()) {
				logger.error("Failed to tokenize string: {}", pennString);
			}
			ParsedTree wrappedTree = new ParsedTree();
			SubTree tree = wrappedTree.new SubTree();
			try {
				tree = tree.readTree(tokenizer.next(), tokenizer);
				wrappedTree.root = tree;
			} catch (Exception e) {
				logger.error("Failed to parse tree:" + pennString, e);
				return null;
			}
			
			ArrayList<String> tokensList = new ArrayList<>();
			for (SubTree leaf : wrappedTree.leaves) {
				tokensList.add(leaf.word);
			}
			wrappedTree.tokens = tokensList.toArray(new String[tokensList.size()]);
			return wrappedTree;
		}

		public class SubTree {

			public ArrayList<SubTree> children = new ArrayList<>();
			public SubTree parent = null;
			public String tag = StringResources.STR_EMPTY;
			public String word = StringResources.STR_EMPTY;
			
			public String toString(){
				return word;
			}

			
			public void writeChildren(SubTree tree, StringWriter sw) {
				if (tree.children.size() == 0) {
					sw.write(tree.word);
					return;
				}
				sw.write("(");
				sw.write(tree.word);
				for (SubTree child : tree.children) {
					sw.write(" ");
					writeChildren(child, sw);
				}
				sw.write(")");
			}


			private SubTree readTree(String token,
					TokenizerAdapter tokenizer) throws Exception {

				String name;
				// a paren starts new tree, a string is a leaf symbol,
				// o.w. IO exception
				if (token == null) {
					return null;
				} else if (token.equals(")")) {
					System.err
							.println("Expecting start of tree; found surplus close parenthesis ')'. Ignoring it.");
					return null;
				} else if (token.equals("(")) {
					// looks at next
					name = tokenizer.peek();

					// checks if it's a normal string and returns it as the
					// label
					if (name.equals("(") || name.equals(")")) {
						name = null;
					} else {
						// get it for real
						name = tokenizer.next();
					}
					SubTree current = new SubTree();
					current.word = name;
					current.children = readTrees(tokenizer);
					for (SubTree child : current.children) {
						child.parent = current;
					}
					return current;
				} else {
					name = token;
					SubTree leaf = new SubTree();
					leaf.word = name;
					leaves.add(leaf);
					return leaf;
				}
			}

			private ArrayList<SubTree> readTrees(
					TokenizerAdapter tokenizer) throws Exception {
				// allocate array list for temporarily storing trees
				ArrayList<SubTree> parseTrees = new ArrayList<SubTree>();
				// until a paren closes all subtrees, keep reading trees
				String nextToken = null;
				String fullToken = "";
				while (tokenizer.hasNext()) {
					nextToken = tokenizer.next();
					if (nextToken.equals(")")) {
						break;
					} else if (nextToken.equals("(")) {
						if (!fullToken.equals("")) {
							parseTrees.add(readTree(fullToken, tokenizer));
							fullToken = "";
						}
						parseTrees.add(readTree(nextToken, tokenizer));
					} else {
						fullToken += (fullToken.equals("") ? "" : " ")
								+ nextToken;
					}
				}
				if (!")".equals(nextToken)) {
					throw (new IOException("Expecting right paren found eof"));
				}
				if (!fullToken.equals("")) {
					parseTrees.add(readTree(fullToken, tokenizer));
				}

				return parseTrees;
			}

		}
	}
}
