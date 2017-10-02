/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.io.Lines;

public class PreprocessorLines extends Lines {
	
	private static Logger logger = LoggerFactory.getLogger(PreprocessorLines.class);

	Lines lines;
	Filter[] filters;

	public PreprocessorLines(Lines lines, Filter ... filters) {
		this.lines = lines;
		this.filters = filters;
	}

	@Override
	public Iterator<String> iterator() {
		return new PIterator(lines);
	}

	public class PIterator implements Iterator<String> {
		
		Iterator<String> ite;
		long count = 0;

		public PIterator(Iterable<String> iterable) {
			ite = iterable.iterator();
		}

		@Override
		public boolean hasNext() {
			return ite.hasNext();
		}

		@Override
		public String next() {
			count++;
			if(count%10000 == 0)
				logger.info("Preprocess {} sentences", count);
			return Preprocessor.preprocess(ite.next(), filters);
		}

		@Override
		public void remove() {
			logger.error("Unsupport operation: remove element");
		}
	}

}
