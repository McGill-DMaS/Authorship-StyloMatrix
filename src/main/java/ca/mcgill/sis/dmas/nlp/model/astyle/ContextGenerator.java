package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.util.List;
import java.util.Map;

import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.model.embedding.Word;

public abstract class ContextGenerator {

	public abstract Iterable<Context> generateContext(Sentence sentence,
			Map<String, Word> wMap);

	public abstract class Context {
		public abstract List<Word> getFullContext();

		public abstract List<Word> getBaseContext();

		public abstract List<Word> getPredictionContext();

	}
	
	public static ContextGenerator newContextGeneratorCBOW(int windowSize) {
		return new ContextGeneratorCBOW(windowSize);
	}

	public static ContextGenerator newContextGeneratorSKIPGRAM(int windowSize) {
		return new ContextGeneratorSKIPGRAM(windowSize);
	}

	public static ContextGenerator newContextGeneratorSKIPGRAM(int windowSize,
			int numberOfSkips) {
		return new ContextGeneratorSKIPGRAM_N(windowSize, numberOfSkips);
	}

	public static ContextGenerator newContextGeneratorTREE(Parser parser) {
		return new ContextGeneratorTREETopDown(parser);
	}
	
	public static ContextGenerator newContextGeneratorEmptyBase(){
		return new ContextGeneratorEmptyBase();
	}

}
