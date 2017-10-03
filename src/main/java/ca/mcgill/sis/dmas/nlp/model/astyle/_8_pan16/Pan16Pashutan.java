package ca.mcgill.sis.dmas.nlp.model.astyle._8_pan16;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import com.google.common.collect.Iterables;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.heap.Ranker;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import dk.dren.hunspell.Hunspell;
import dk.dren.hunspell.Hunspell.Dictionary;
import org.slf4j.LoggerFactory;
import ca.mcgill.sis.dmas.nlp.corpus.parser.HunspellUtils;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NLPUtilsInitializer;
import ca.mcgill.sis.dmas.nlp.corpus.parser.StopWords;

public class Pan16Pashutan {

	private static Logger logger = LoggerFactory.getLogger(Pan16Pashutan.class);

	private Dictionary dict = null;
	private HashSet<String> stopwords = null;

	public Pan16Pashutan(Language language) throws FileNotFoundException, UnsupportedEncodingException,
			UnsatisfiedLinkError, UnsupportedOperationException {
		this.dict = Hunspell.getInstance().getDictionary(HunspellUtils.getDictionary(language));
		this.stopwords = StopWords.getStopWords(language);
	}

	private Map<String, double[]> docRepresentations = new HashMap<String, double[]>();

	public void addUnigramsCount(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		// replace "#URL"
		// remove tokens started with @
		// remove tokens started with #
		// remove all retweet. (Text started with "rt:" (lowered case))
		// remove accent
		// remove non-alphabetic characters
		// remove stop-words

		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs)
			if (!doc.rawContent.startsWith("rc:"))
				for (String tkn : Iterables.concat(doc.sentences)) {
					tkn = tkn.trim().toLowerCase();
					if (!tkn.startsWith("@") && !tkn.startsWith("#")) {
						if (stopwords.contains(tkn))
							continue;
						tkn = Normalizer.normalize(tkn, Normalizer.Form.NFD);
						tkn = tkn.replaceAll("\\p{M}", "");
						counts.compute(tkn, (k, v) -> v == null ? 1d : v + 1);
					}
				}

		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> unigrams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, unigrams.size())
				.mapToObj(ind -> new EntryPair<>(unigrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

		for (Document doc : docs) {
			double[] vec = new double[unigrams.size()];
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				tkn = Normalizer.normalize(tkn, Normalizer.Form.NFD);
				tkn = tkn.replaceAll("\\p{M}", "");
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

	public void addMisSpellRatio(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		for (Document doc : docs) {

			double[] ratio = new double[1];
			double total = 0;
			try {
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
			} catch (Exception e) {
				logger.error("Failed to process a document for misspell feature.", e);
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

	public void addPunctuationRatio(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		char[] features = new char[] { ',', '.', '!', '?' };
		for (Document doc : docs) {
			String[] sents = SentenceDetector.sentenceDetectorStandford.detectSentences(doc.rawContent);
			double[] vec = new double[features.length];
			for (String sent : sents) {
				for (int i = 0; i < features.length; ++i) {
					for (char chr : sent.toCharArray()) {
						if (chr == features[i])
							vec[i]++;
					}
				}
			}
			if (sents.length != 0)
				MathUtilities.div(vec, sents.length);
			double[] old = docRepresentations.get(doc.id);
			if (old == null)
				old = vec;
			else
				old = MathUtilities.concate(old, vec);
			docRepresentations.put(doc.id, old);
		}
	}

	public void addBigramsCount(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		// replace "#URL"
		// remove tokens started with @
		// remove tokens started with #
		// remove all retweet. (Text started with "rt:" (lowered case))
		// remove accent
		// remove non-alphabetic characters

		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs) {
			String prev = "";
			if (!doc.rawContent.startsWith("rc:"))
				for (String tkn : Iterables.concat(doc.sentences)) {
					tkn = tkn.trim().toLowerCase();
					if (!tkn.startsWith("@") && !tkn.startsWith("#")) {
						tkn = Normalizer.normalize(tkn, Normalizer.Form.NFD);
						tkn = tkn.replaceAll("\\p{M}", "");
						if (prev.length() < 1)
							prev = tkn;
						else {
							String new_prev = tkn;
							tkn = prev + "-" + tkn;
							counts.compute(tkn, (k, v) -> v == null ? 1d : v + 1);
							prev = new_prev;
						}
					}
				}
		}

		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> bigrams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, bigrams.size())
				.mapToObj(ind -> new EntryPair<>(bigrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

		for (Document doc : docs) {
			double[] vec = new double[bigrams.size()];
			String prev = "";
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				tkn = Normalizer.normalize(tkn, Normalizer.Form.NFD);
				tkn = tkn.replaceAll("\\p{M}", "");
				if (prev.length() < 1)
					prev = tkn;
				else {
					String new_prev = tkn;
					tkn = prev + "-" + tkn;
					if (indMap.containsKey(tkn))
						vec[indMap.get(tkn)]++;
					prev = new_prev;
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

	public void addChar4GramCount(Iterable<Document> docs, Map<String, double[]> docRepresentations) {
		// replace "#URL"
		// remove tokens started with @
		// remove tokens started with #

		HashMap<String, Double> counts = new HashMap<>();
		for (Document doc : docs) {
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (!tkn.startsWith("@") && !tkn.startsWith("#")) {
					LinkedList<Character> queue = new LinkedList<>();
					for (char chr : tkn.toCharArray())
						if (queue.size() < 4)
							queue.addLast(chr);
						else {
							String key = StringResources.JOINER_TOKEN_CSV.join(queue);
							counts.compute(key, (k, v) -> v == null ? 1d : v + 1);
							queue.removeFirst();
							queue.addLast(chr);
						}
				}
			}
		}

		Ranker<String> rank = new Ranker<>(1000);
		rank.push(counts);
		List<String> c4grams = new ArrayList<>(rank.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, c4grams.size())
				.mapToObj(ind -> new EntryPair<>(c4grams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

		for (Document doc : docs) {
			double[] vec = new double[c4grams.size()];
			for (String tkn : Iterables.concat(doc.sentences)) {
				tkn = tkn.trim().toLowerCase();
				if (!tkn.startsWith("@") && !tkn.startsWith("#")) {
					LinkedList<Character> queue = new LinkedList<>();
					for (char chr : tkn.toCharArray())
						if (queue.size() < 4)
							queue.addLast(chr);
						else {
							String key = StringResources.JOINER_TOKEN_CSV.join(queue);
							if (indMap.containsKey(key))
								vec[indMap.get(key)]++;
							queue.removeFirst();
							queue.addLast(chr);
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

	public void train(Iterable<Document> docs) {
		docRepresentations = new HashMap<>();
		addPunctuationRatio(docs, docRepresentations);
		addMisSpellRatio(docs, docRepresentations);
		addUnigramsCount(docs, docRepresentations);
		addBigramsCount(docs, docRepresentations);
		addChar4GramCount(docs, docRepresentations);
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
		document.rawContent = "The actors wanted to see if the pact seemed like an old-fashioned one The actors wanted to seeee if the pact seemed like an old-fashioned one.";
		document.sentences = new ArrayList<>(
				Arrays.asList(new Sentence(document.rawContent, Tokenizer.tokenizerDefault)));
		Pan16Pashutan model = new Pan16Pashutan(Language.english);
		model.train(Arrays.asList(document));
		System.out.println(Arrays.toString(model.getDocRepresentations().get("t1")));
	}

}
