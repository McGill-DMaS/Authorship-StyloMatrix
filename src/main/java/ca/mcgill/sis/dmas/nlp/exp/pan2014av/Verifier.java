package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.util.Set;

public interface Verifier {
	public double similarity(String doc1key, String doc2key);

	public Set<String> getKeys();
}