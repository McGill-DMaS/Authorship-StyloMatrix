/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public class ParagraphsFromLines extends Paragraphs {

	private static Logger logger = LoggerFactory
			.getLogger(ParagraphsFromLines.class);
	Lines lines;
	
	String seperator = StringResources.STR_PARAGRAPHBREAK;
	Tokenizer tokenizer = null;
	Parser parser = null;
	SentenceDetector detector = null;

	public ParagraphsFromLines(Lines lines_, String seperatorLine) {
		lines = lines_;
		this.seperator = seperatorLine;
	}
	
	public ParagraphsFromLines(Lines lines_, SentenceDetector detector, Tokenizer tokenizer) {
		lines = lines_;
		this.detector = detector;
		this.tokenizer = tokenizer;
	}
	
	public ParagraphsFromLines(Lines lines_, SentenceDetector detector, Tokenizer tokenizer, Parser parser) {
		lines = lines_;
		this.detector = detector;
		this.tokenizer = tokenizer;
		this.parser = parser;
	}

	@Override
	public Iterator<Paragraph> iterator() {
		return new ParagraphIterator();
	}


	public class ParagraphIterator implements Iterator<Paragraph> {

		Iterator<String> ite = lines.iterator();

		@Override
		public boolean hasNext() {
			return ite.hasNext();
		}

		@Override
		public Paragraph next() {
			List<Sentence> sentences = new ArrayList<>();
			if (tokenizer == null || detector == null) {
				while (ite.hasNext()) {
					String line = ite.next();
					if (line.trim().equals(seperator))
						break;
					sentences.add(new Sentence(line));
				}
				return new Paragraph(sentences.toArray(new Sentence[sentences
						.size()]));
			}else{
				String line = ite.next();
				if(parser == null)
					return new Paragraph(line, detector, tokenizer);
				else
					return new Paragraph(line, detector, tokenizer, parser);
			}
		}

		@Override
		public void remove() {
			logger.error("Unsupport operation. Cannot remove paragraph.");
		}

	}

}
