package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.mcgill.sis.dmas.io.collection.list.MergedList;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.embedding.Word;

public class ContextGeneratorCBOW extends ContextGenerator {

	public ContextGeneratorCBOW(int windowSize) {
		window = windowSize;
	}
	
	int window = 0;
	
	@Override
	public String toString(){
		return "cbow-"+window;
	}

	@Override
	public Iterable<Context> generateContext(Sentence sentence,
			Map<String, Word> wMap) {
		
		ContextCBOWIterable contextIterable = new ContextCBOWIterable();
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

	private class ContextCBOWIterable implements Iterable<Context> {

		List<Word> tokens;
		int start = 0;
		
		@Override
		public Iterator<Context> iterator() {
			return new ContextCBOWIterator();
		}

		private class ContextCBOWIterator implements Iterator<Context> {

			@Override
			public boolean hasNext() {
				if (start >= 0 && start < tokens.size())
					return true;
				return false;
			}

			@Override
			public Context next() {
				Context context = new ContextCBOW();
				start++;
				return context;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private class ContextCBOW extends Context {

				int lstart = start;
				int from = start - window;
				int to = start + window + 1;
				int r_start = start + 1;

				public ContextCBOW() {
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

					List<Word> left = tokens.subList(from, lstart);
					List<Word> right = tokens.subList(r_start, to);
					if (left.size() == 0)
						return right;
					if (right.size() == 0)
						return left;
					else
						return new MergedList<Word>(left, right);
				}

				@Override
				public List<Word> getPredictionContext() {
					return tokens.subList(lstart, r_start);
				}

			}

		}

	}

	public static void main(String[] args) {
		Sentence sentence = new Sentence("it is a great great day !");
		HashMap<String, Word> map = new HashMap<>();
		for (String token : sentence) {
			Word word = new Word();
			word.word = token;
			word.freq = 5;
			map.put(token, word);
		}
		ContextGenerator cg = new ContextGeneratorCBOW(2);
		for (Context context : cg.generateContext(sentence, map)) {
			System.out.println(context.getFullContext().toString());
			System.out.println(context.getBaseContext().toString());
			System.out.println(context.getPredictionContext().toString());
		}
	}

}
