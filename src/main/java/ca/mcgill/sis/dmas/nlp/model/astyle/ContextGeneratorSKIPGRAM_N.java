package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.io.collection.list.MergedList;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.embedding.Word;

public class ContextGeneratorSKIPGRAM_N extends ContextGenerator {

	private static Logger logger = LoggerFactory
			.getLogger(ContextGeneratorSKIPGRAM_N.class);

	public ContextGeneratorSKIPGRAM_N(int windowSize, int numberOfSkips) {
		window = windowSize;
		this.numberOfSkip = numberOfSkips;
		if (numberOfSkips < 0) {
			logger.error("Invalid parameter: number of skips {}; setting to 1",
					numberOfSkips);
			this.numberOfSkip = 1;
		}
	}

	int window = 0;
	int numberOfSkip = 1;

	@Override
	public String toString() {
		return "skipws-" + window + "-" + numberOfSkip;
	}

	@Override
	public Iterable<Context> generateContext(Sentence sentence,
			Map<String, Word> wMap) {
		ContextSKIPGRAMIterable contextIterable = new ContextSKIPGRAMIterable();
		contextIterable.tokens = new ArrayList<>(sentence.tokens.length);
		for (int i = 0; i < sentence.tokens.length; i++) {
			Word word = wMap.get(sentence.tokens[i]);
			if (word == null)
				continue;
			contextIterable.tokens.add(word);
		}
		contextIterable.start = 0;
		return contextIterable;
	}

	private class ContextSKIPGRAMIterable implements Iterable<Context> {

		List<Word> tokens;
		int start = 0;

		@Override
		public Iterator<Context> iterator() {
			return new ContextSKIPGRAMIterator();
		}

		private class ContextSKIPGRAMIterator implements Iterator<Context> {

			@Override
			public boolean hasNext() {
				if (start >= 0 && start <= tokens.size() - numberOfSkip)
					return true;
				return false;
			}

			@Override
			public Context next() {
				Context context = new ContextSKIPGRAM();
				start++;
				return context;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private class ContextSKIPGRAM extends Context {

				int lstart = start;
				int from = start - window;
				int to = start + numberOfSkip + window;
				int r_start = start + numberOfSkip;

				public ContextSKIPGRAM() {
					from = from > 0 ? from : 0;
					to = to <= tokens.size() ? to : tokens.size();
					r_start = r_start <= tokens.size() ? r_start : tokens
							.size();
				}

				@Override
				public List<Word> getFullContext() {
					return tokens.subList(from, to);
				}

				@Override
				public List<Word> getBaseContext() {
					return tokens.subList(lstart, r_start);
				}

				@Override
				public List<Word> getPredictionContext() {
					List<Word> left = tokens.subList(from, lstart);
					List<Word> right = tokens.subList(r_start, to);
					return new MergedList<Word>(left, right);
				}

			}

		}

	}

}
