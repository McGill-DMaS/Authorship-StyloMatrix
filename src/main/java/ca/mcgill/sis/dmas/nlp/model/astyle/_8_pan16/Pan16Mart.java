package ca.mcgill.sis.dmas.nlp.model.astyle._8_pan16;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import ca.mcgill.sis.dmas.nlp.model.astyle._7_typedNgram.TypedNgram;
import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.heap.Ranker;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsOpennlp;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsStandford;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import dk.dren.hunspell.Hunspell;
import dk.dren.hunspell.Hunspell.Dictionary;
import ca.mcgill.sis.dmas.nlp.corpus.parser.HunspellUtils;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NLPUtilsInitializer;

public class Pan16Mart {

	private Dictionary dict = null;

	public Pan16Mart(Language language) throws FileNotFoundException, UnsupportedEncodingException,
			UnsatisfiedLinkError, UnsupportedOperationException {
		this.dict = Hunspell.getInstance().getDictionary(HunspellUtils.getDictionary(language));
	}

	private Map<String, double[]> docRepresentations = new HashMap<String, double[]>();

	public void addMisSpellRatio(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		// replace digit
		// remove tag
		// replace url (already done)

		for (Document doc : docs) {
			double[] ratio = new double[1];
			double total = 0;
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				total += 1;
				if (dict.misspelled(tkn))
					ratio[0] += 1;
			}
			if (total != 0)
				ratio[0] /= total;
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addCharacterNgram(Iterable<Document> docs, Map<String, double[]> docRepresentations, int n) {
		// replace digit
		// remove tag
		// replace url (already done)

		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs) {
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				LinkedList<Character> queue = new LinkedList<>();
				for (char chr : tkn.toCharArray())
					if (queue.size() < n)
						queue.addLast(chr);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						counts.compute(key, (k, v) -> v == null ? 1d : v + 1);
						queue.removeFirst();
						queue.addLast(chr);
					}
			}
		}
		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> cngrams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, cngrams.size())
				.mapToObj(ind -> new EntryPair<>(cngrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));
		for (Document doc : docs) {
			double[] vec = new double[cngrams.size()];
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				LinkedList<Character> queue = new LinkedList<>();
				for (char chr : tkn.toCharArray())
					if (queue.size() < n)
						queue.addLast(chr);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						if (indMap.containsKey(key))
							vec[indMap.get(key)]++;
						queue.removeFirst();
						queue.addLast(chr);
					}
			}
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}

	}

	public void addPOSNgram(Iterable<Document> docs, Map<String, double[]> docRepresentations, int n) {
		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs) {
			for (Sentence sent : doc.sentences_tags) {
				LinkedList<String> queue = new LinkedList<>();
				for (String tkn : sent) {
					tkn = tkn.trim().toLowerCase();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (queue.size() < n)
						queue.addLast(tkn);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						counts.compute(key, (k, v) -> v == null ? 1d : v + 1);
						queue.removeFirst();
						queue.addLast(tkn);
					}
				}
			}
		}
		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> cngrams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, cngrams.size())
				.mapToObj(ind -> new EntryPair<>(cngrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));
		for (Document doc : docs) {
			double[] vec = new double[cngrams.size()];
			for (Sentence sent : doc.sentences_tags) {
				LinkedList<String> queue = new LinkedList<>();
				for (String tkn : sent) {
					tkn = tkn.trim().toLowerCase();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (queue.size() < n)
						queue.addLast(tkn);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						if (indMap.containsKey(key))
							vec[indMap.get(key)]++;
						queue.removeFirst();
						queue.addLast(tkn);
					}
				}
			}
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addNgram(Iterable<Document> docs, Map<String, double[]> docRepresentations, int n) {
		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs) {
			for (Sentence sent : doc.sentences) {
				LinkedList<String> queue = new LinkedList<>();
				for (String tkn : sent) {
					tkn = tkn.trim().toLowerCase();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (queue.size() < n)
						queue.addLast(tkn);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						counts.compute(key, (k, v) -> v == null ? 1d : v + 1);
						queue.removeFirst();
						queue.addLast(tkn);
					}
				}
			}
		}
		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> cngrams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, cngrams.size())
				.mapToObj(ind -> new EntryPair<>(cngrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));
		for (Document doc : docs) {
			double[] vec = new double[cngrams.size()];
			for (Sentence sent : doc.sentences) {
				LinkedList<String> queue = new LinkedList<>();
				for (String tkn : sent) {
					tkn = tkn.trim().toLowerCase();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (queue.size() < n)
						queue.addLast(tkn);
					else {
						String key = StringResources.JOINER_TOKEN_CSV.join(queue);
						if (indMap.containsKey(key))
							vec[indMap.get(key)]++;
						queue.removeFirst();
						queue.addLast(tkn);
					}
				}
			}
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addStartsWithCapital(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {
			double[] ratio = new double[] { 0. };
			for (Sentence sent : doc.sentences) {
				if (sent.tokens != null && sent.tokens.length > 0 && sent.tokens[0] != null
						&& sent.tokens[0].length() > 0 && Character.isUpperCase(sent.tokens[0].charAt(0)))
					ratio[0]++;
			}
			if (doc.sentences.size() != 0)
				ratio[0] /= doc.sentences.size();
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addCapitalizedTokens(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {
			double[] ratio = new double[1];
			double total = 0;
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				total += 1;
				if (tkn.length() > 0 && Character.isUpperCase(tkn.charAt(0)))
					ratio[0] += 1;
			}
			if (total != 0)
				ratio[0] /= total;
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addCapitalizedRatio(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {
			double[] ratio = new double[1];
			for (Sentence sent : doc.sentences) {
				int cap = 0;
				int len = 0;
				for (String tkn : sent) {
					tkn = tkn.trim();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (tkn.matches(StringResources.REGEX_NUMBER))
						continue;
					for (char chr : tkn.toCharArray()) {
						len += 1;
						if (Character.isUpperCase(chr))
							cap += 1;
					}
				}
				ratio[0] += cap * 1.0 / len;
			}
			if (doc.sentences.size() != 0)
				ratio[0] /= doc.sentences.size();
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addEndWithPunctuation(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		Set<Character> punctuations = Sets.newHashSet('.', ',', '?', '!');
		for (Document doc : docs) {
			double[] ratio = new double[] { 0. };
			for (Sentence sent : doc.sentences) {
				if (sent.tokens != null && sent.tokens.length > 0 && sent.tokens[sent.tokens.length - 1] != null
						&& sent.tokens[sent.tokens.length - 1].length() > 0) {
					char[] chars = sent.tokens[sent.tokens.length - 1].toCharArray();
					if (punctuations.contains(chars[chars.length - 1]))
						ratio[0]++;
				}
			}
			if (doc.sentences.size() != 0)
				ratio[0] /= doc.sentences.size();
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addPunctuationRatio(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		char[] features = new char[] { ',', '.', ';', '!', '?', '-' };
		for (Document doc : docs) {
			double[] vec = new double[features.length];
			for (Sentence sent : doc.sentences) {
				double[] ratio = new double[features.length];
				int total = 0;
				for (String tkn : sent) {
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (tkn.matches(StringResources.REGEX_NUMBER))
						continue;
					total += tkn.length();
					for (int i = 0; i < features.length; ++i) {
						for (char chr : tkn.toCharArray()) {
							if (chr == features[i])
								ratio[i]++;
						}
					}
				}
				if (total != 0)
					MathUtilities.div(ratio, total);
				MathUtilities.add(vec, ratio);
			}
			if (doc.sentences.size() != 0)
				MathUtilities.div(vec, doc.sentences.size());
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addAvgWordLenSentLen(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {
			double[] vec = new double[2];
			double total_tkn = 0;
			for (Sentence sent : doc.sentences) {
				for (String tkn : sent) {
					tkn = tkn.trim();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (tkn.matches(StringResources.REGEX_NUMBER))
						continue;
					total_tkn += 1;
					vec[0] += 1;
					vec[1] += tkn.length();
				}
			}
			if (doc.sentences.size() != 0)
				vec[0] /= doc.sentences.size();
			if (total_tkn != 0)
				vec[1] /= total_tkn;
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addVocabRichness(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {
			double total_tkn = 0;
			HashMap<String, Integer> count = new HashMap<>();
			for (Sentence sent : doc.sentences) {
				for (String tkn : sent) {
					tkn = tkn.trim();
					if (tkn.startsWith("<") && tkn.endsWith(">"))
						continue;
					if (tkn.matches(StringResources.REGEX_NUMBER))
						continue;
					total_tkn++;
					count.compute(tkn, (k, v) -> v == null ? 1 : v + 1);
				}
			}
			long total_unique = count.values().stream().filter(val -> val == 1).count();
			double[] vec = new double[] { total_tkn == 0 ? 0 : total_unique / total_tkn };
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addFuncWords(Iterable<Document> docs, Map<String, double[]> docRepresentations, int topk,
			Map<String, String> docLabels) {
		HashSet<String> featureSet = new HashSet<>();
		for (String key : new HashSet<>(docLabels.values())) {
			HashMap<String, double[]> stats = new HashMap<>();
			for (Document doc : docs) {
				if (docLabels.containsKey(doc.id))
					for (Sentence sent : doc.sentences) {
						for (String tkn : sent) {
							tkn = tkn.trim();
							if (tkn.startsWith("<") && tkn.endsWith(">"))
								continue;
							if (tkn.matches(StringResources.REGEX_NUMBER))
								continue;
							double[] stat_tkn = stats.get(key);
							if (stat_tkn == null)
								stat_tkn = new double[2];
							if (docLabels.get(doc.id).equals(key))
								stat_tkn[0]++;
							else
								stat_tkn[1]++;
							stats.put(tkn, stat_tkn);
						}
					}
			}
			Ranker<String> heap = new Ranker<>(topk);
			for (String tkn : stats.keySet()) {
				double[] val = stats.get(tkn);
				if (val[1] != 0)
					heap.push(val[0] / val[1], tkn);
			}
			featureSet.addAll(heap.getKeys());
		}

		List<String> features = new ArrayList<>(featureSet);
		Map<String, Integer> indMap = IntStream.range(0, features.size())
				.mapToObj(ind -> new EntryPair<>(features.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));
		for (Document doc : docs) {
			double[] vec = new double[features.size()];
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				if (indMap.containsKey(tkn))
					vec[indMap.get(tkn)]++;
			}
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}

	}

	public void addEmoticons(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		// ratio of emoticons
		for (Document doc : docs) {
			double[] ratio = new double[1];
			double total = 0;
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				total += 1;
				if (tkn.length() > 1) {
					if (tkn.startsWith(":") || tkn.startsWith(";") || tkn.startsWith("=") || tkn.endsWith(":")
							|| tkn.endsWith(";") || tkn.endsWith("="))
						// or reverse
						ratio[0] += 1;
				}
			}
			if (total != 0)
				ratio[0] /= total;
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}

		// ratio of nose, reverse, happy emoticons:
		for (Document doc : docs) {
			double[] ratio = new double[3];
			double total = 0;
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim();
				if (tkn.startsWith("<") && tkn.endsWith(">"))
					continue;
				if (tkn.matches(StringResources.REGEX_NUMBER))
					continue;
				if (tkn.length() > 1)
					if (tkn.startsWith(":") || tkn.startsWith(";") || tkn.startsWith("=") || tkn.endsWith(":")
							|| tkn.endsWith(";") || tkn.endsWith("=")) {
						total++;
						if (tkn.contains("-"))
							ratio[0]++;
						if (tkn.endsWith(":") || tkn.endsWith(";") || tkn.endsWith("="))
							ratio[1]++;
						if (tkn.contains(")") || tkn.contains("]") || tkn.contains("P") || tkn.contains("D"))
							ratio[2]++;
					}
			}
			if (total != 0)
				MathUtilities.div(ratio, total);
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = ratio;
			else
				old = MathUtilities.concate(old, ratio);
			docRepresentations.put(doc.id, old);
		}
	}

	public void train(Iterable<Document> docs, Map<String, String> docLabels) {
		docRepresentations = new HashMap<>();
		addNgram(docs, docRepresentations, 1);
		addNgram(docs, docRepresentations, 2);
		addNgram(docs, docRepresentations, 3);
		addCharacterNgram(docs, docRepresentations, 2);
		addCharacterNgram(docs, docRepresentations, 3);
		addCharacterNgram(docs, docRepresentations, 4);
		addCharacterNgram(docs, docRepresentations, 5);
		addPOSNgram(docs, docRepresentations, 1);
		addPOSNgram(docs, docRepresentations, 2);
		addPOSNgram(docs, docRepresentations, 3);

		addStartsWithCapital(docs, docRepresentations);
		addCapitalizedTokens(docs, docRepresentations);
		addCapitalizedRatio(docs, docRepresentations);
		addEndWithPunctuation(docs, docRepresentations);
		addPunctuationRatio(docs, docRepresentations);
		addAvgWordLenSentLen(docs, docRepresentations);
		addMisSpellRatio(docs, docRepresentations);
		addVocabRichness(docs, docRepresentations);
		addFuncWords(docs, docRepresentations, 2500, docLabels);
		addEmoticons(docs, docRepresentations);
		addMisSpellRatio(docs, docRepresentations);
	}

	public Map<String, double[]> getDocRepresentations() {
		return this.docRepresentations;
	}

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException,
			UnsatisfiedLinkError, UnsupportedOperationException {
		String NLP_UTIL_PATH = "E:/authorship-cyb/nlps/";
		NLPUtilsInitializer.initialize(NLP_UTIL_PATH);

		Document document = new Document();
		document.id = "t1";
		document.rawContent = "The actors wanted to see if the pact seemed like an old-fashioned one The actors wanted to see if the pact seemed like an old-fashioned one.";
		document.sentences = new ArrayList<>(
				Arrays.asList(new Sentence(document.rawContent, Tokenizer.tokenizerDefault)));
		Pan16Mart model = new Pan16Mart(Language.english);
		HashMap<String, String> labels = new HashMap<>();
		labels.put(document.id, "male");
		model.train(Arrays.asList(document), labels);
		System.out.println(Arrays.toString(model.getDocRepresentations().get("t1")));
	}

}
