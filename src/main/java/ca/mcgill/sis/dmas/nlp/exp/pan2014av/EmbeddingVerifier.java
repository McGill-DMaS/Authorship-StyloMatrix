package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;

public class EmbeddingVerifier implements Verifier {

	private static Logger logger = LoggerFactory.getLogger(EmbeddingVerifier.class);

	public Map<String, double[]> embedding;

	public EmbeddingVerifier(Map<String, double[]> embedding) {
		this.embedding = embedding;
	}

	@Override
	public double similarity(String doc1key, String doc2key) {
		double[] v1 = embedding.get(doc1key);
		double[] v2 = embedding.get(doc2key);
		if (v1 == null) {
			logger.warn("Missing vec for key {}", doc1key);
			return 0;
		}
		if (v2 == null) {
			logger.warn("Missing vec for key {}", doc2key);
			return 0;
		}
		return MathUtilities.dot(v1, v2);
	}

	@Override
	public Set<String> getKeys() {
		return embedding.keySet();
	}

}
