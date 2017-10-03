package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.mcgill.sis.dmas.io.collection.list.MergedList;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.ContextGenerator.Context;
import ca.mcgill.sis.dmas.nlp.model.embedding.Word;

public class ContextGeneratorEmptyBase extends ContextGenerator {
	public ContextGeneratorEmptyBase() {
	}

	static final ArrayList<Word> empty_list = new ArrayList<>();
	


	@Override
	public String toString() {
		return "embase";
	}

	@Override
	public Iterable<Context> generateContext(Sentence sentence,
			Map<String, Word> wMap) {
		ContextEmptyBaseIterable contextIterable = new ContextEmptyBaseIterable();
		contextIterable.tokens = new ArrayList<>(sentence.tokens.length);
		for (int i = 0; i < sentence.tokens.length; i++) {
			Word word = wMap.get(sentence.tokens[i]);
			if (word == null)
				continue;
			contextIterable.tokens.add(word);
		}
		return contextIterable;
	}

	private class ContextEmptyBaseIterable implements Iterable<Context> {

		List<Word> tokens;
		
		
		@Override
		public Iterator<Context> iterator() {
			return new ContextSKIPGRAMIterator();
		}

		private class ContextSKIPGRAMIterator implements Iterator<Context> {

			@Override
			public boolean hasNext() {
				if (tokens != null)
					return true;
				return false;
			}

			@Override
			public Context next() {
				Context context = new ContextSKIPGRAM();
				tokens = null;
				return context;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			private class ContextSKIPGRAM extends Context {

				List<Word> tokens_cache = tokens;

				public ContextSKIPGRAM() {
				}

				@Override
				public List<Word> getFullContext() {
					return tokens_cache;
				}

				@Override
				public List<Word> getBaseContext() {
					return empty_list;
				}

				@Override
				public List<Word> getPredictionContext() {
					return tokens_cache;
				}

			}

		}

	}

}
