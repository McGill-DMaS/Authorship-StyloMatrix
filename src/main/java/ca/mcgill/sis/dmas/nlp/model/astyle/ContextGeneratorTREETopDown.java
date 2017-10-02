package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ca.mcgill.sis.dmas.io.collection.list.MergedList;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser.ParsedTree;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser.ParsedTree.SubTree;
import ca.mcgill.sis.dmas.nlp.model.embedding.Word;

public class ContextGeneratorTREETopDown extends ContextGenerator {

	public ContextGeneratorTREETopDown(Parser parser) {
		this.parser = parser;
	}

	Parser parser;
	


	@Override
	public String toString() {
		return "parser";
	}

	@Override
	public Iterable<Context> generateContext(Sentence sentence,
			Map<String, Word> wMap) {
		if (sentence.tree == null)
			sentence.parse(parser);
		ContextTREEIterable iterable = new ContextTREEIterable(sentence.tree,
				wMap);
		return iterable;
	}

	private class ContextTREEIterable implements Iterable<Context> {

		ParsedTree tree;
		Map<String, Word> wMap;

		List<Word> emptyList = new ArrayList<>();

		@Override
		public Iterator<Context> iterator() {
			return new ContextTREEIterator();
		}

		ArrayList<List<List<Word>>> contexts = new ArrayList<>();

		public ContextTREEIterable(ParsedTree tree, Map<String, Word> wMap) {
			this.tree = tree;
			this.wMap = wMap;
			topDown(this.tree.root);
		}

		public List<Word> topDown(SubTree tree) {
			if (tree.children.size() == 0) {
				Word word = wMap.get(tree.word);
				if (word == null)
					return emptyList;
				else
					return Arrays.asList(word);
			}
			ArrayList<List<Word>> context = new ArrayList<>();
			ArrayList<Word> fullContext = new ArrayList<>();
			for (SubTree subTree : tree.children) {
				List<Word> list = topDown(subTree);
				context.add(list);
				fullContext.addAll(list);
			}
			if (context.size() > 1)
				contexts.add(context);
			return fullContext;
		}

		private class ContextTREEIterator implements Iterator<Context> {

			Iterator<List<List<Word>>> ite = contexts.iterator();
			int iteIndex = 0;
			List<List<Word>> current = null;

			@Override
			public boolean hasNext() {
				while (current == null || iteIndex >= current.size()) {
					if (!ite.hasNext())
						return false;
					current = ite.next();
					iteIndex = 0;
				}
				return true;
			}

			@Override
			public Context next() {
				ContextTREETD returnContext = new ContextTREETD();
				iteIndex++;
				return returnContext;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private class ContextTREETD extends Context {

				int l_index = iteIndex;

				@Override
				public List<Word> getFullContext() {
					return new MergedList<>(true, current);
				}

				@Override
				public List<Word> getBaseContext() {
					List<List<Word>> left = current.subList(0, l_index);
					List<List<Word>> right = current.subList(l_index + 1,
							current.size());
					return new MergedList<Word>(true, new MergedList<>(true, left,
							right));
				}

				@Override
				public List<Word> getPredictionContext() {
					return new MergedList<>(true, current.subList(l_index,
							l_index + 1));
				}

			}

		}

	}

}
