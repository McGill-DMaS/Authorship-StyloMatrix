package ca.mcgill.sis.dmas.nlp.model.astyle._6_LSA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;

public class WordIndexMap {

	private HashMap<String, Integer> word2IndMap = new HashMap<>();
	private ArrayList<String> ind2WordMap = new ArrayList<>();
	private HashSet<String> dict;

	public WordIndexMap(HashSet<String> keys) {
		this.dict = keys;
	}

	public int dim() {
		return ind2WordMap.size();
	}

	public int getDim(String word, boolean update) {
		Integer ind = word2IndMap.get(word);
		if (ind == null) {
			if (update && (this.dict == null || this.dict.contains(word))) {
				ind = ind2WordMap.size();
				ind2WordMap.add(word);
				word2IndMap.put(word, ind);
			} else {
				ind = -1;
			}
		}
		return ind;
	}

	public String getWord(int ind) {
		return ind2WordMap.get(ind);
	}

	public HashMap<Integer, Double> countWordInDoc(Document doc, boolean updateVocab) {
		HashMap<Integer, Double> counter = new HashMap<>();
		doc.tokens().map(tkn -> getDim(tkn.toLowerCase(), updateVocab)).filter(ind -> ind != -1)
				.forEach(ind -> counter.compute(ind, (k, v) -> v == null ? 1.0 : (v + 1.0)));
		return counter;
	}

}
