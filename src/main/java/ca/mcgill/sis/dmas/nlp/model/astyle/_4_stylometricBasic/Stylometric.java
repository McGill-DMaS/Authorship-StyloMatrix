package ca.mcgill.sis.dmas.nlp.model.astyle._4_stylometricBasic;

import java.util.Map;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;

public class Stylometric {

	private FMFeatureMgr mgr;

	public Stylometric(StylometricParam param) {
		mgr = new FMFeatureMgr(param);
	}

	public static class StylometricParam extends Param {

		private static final long serialVersionUID = -5139280465741063654L;

		public static enum NGramRanker {
			infoGain, Frequency
		}

		public int Ngram_TopK = 3000;
		public NGramRanker ranker = NGramRanker.Frequency;
		public boolean inMem = true;
		public boolean posOnly = false;

		public void setNgramTopK(int topk) {
			Ngram_TopK = topk;
		}

		public void setRanker(NGramRanker rankerType) {
			ranker = rankerType;
		}

	}

	private Map<String, double[]> docEmbedding;

	public void train(Iterable<Document> docs) {
		mgr.prepare(docs);
		this.docEmbedding = mgr.extractFeatures(docs);
	}

	public Map<String, double[]> getDocEmbedding() {
		return this.docEmbedding;
	}

	public Map<String, double[]> inferNewDocEmbedding(Iterable<Document> docs) {
		return mgr.extractFeatures(docs);
	}

}
