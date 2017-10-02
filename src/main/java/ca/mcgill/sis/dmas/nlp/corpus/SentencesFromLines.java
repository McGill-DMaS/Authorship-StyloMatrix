/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.Iterator;

import java_cup.shift_action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

/**
 * Provide iterator collection of sentences Non-thread-safe The read lines must
 * be pre processed lines..
 * 
 * @author steven
 * 
 */
public class SentencesFromLines extends Sentences {

	Logger logger = LoggerFactory.getLogger(SentencesFromLines.class);

	private Lines lines_;
	private Tokenizer tokenizer = null;
	private Parser parser = null;

	public SentencesFromLines(Lines lines) {
		lines_ = lines;
	}
	
	public SentencesFromLines(Lines lines, Tokenizer tokenizer) {
		lines_ = lines;
		this.tokenizer = tokenizer;
	}

	public SentencesFromLines(Lines lines, Tokenizer tokenizer, Parser parser) {
		lines_ = lines;
		this.tokenizer = tokenizer;
		this.parser = parser;
	}

	@Override
	public Iterator<Sentence> iterator() {
		return new SentenceIterator();
	}

	public class SentenceIterator implements Iterator<Sentence> {

		private Iterator<String> linesIterator = null;

		public SentenceIterator() {
			if (lines_ != null)
				linesIterator = lines_.iterator();
		}

		@Override
		public boolean hasNext() {
			if (linesIterator == null)
				return false;
			return linesIterator.hasNext();
		}

		@Override
		public Sentence next() {
			String line = linesIterator.next();
			if (tokenizer == null) {
				Sentence sentence = new Sentence(line);
				return sentence;
			} else if(parser == null) {
				Sentence sentence = new Sentence(line, tokenizer);
				return sentence;
			} else {
				Sentence sentence = new Sentence(line, tokenizer, parser);
				return sentence;
			}
		}

		@Override
		public void remove() {
			logger.error("Unable to remove element. This is an immutable iterator.");
		}

	}
}
