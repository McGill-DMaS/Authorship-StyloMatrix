package ca.mcgill.sis.dmas.nlp.model.astyle._7_typedNgram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.heap.Ranker;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsOpennlp;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsStandford;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;

public class TypedNgram {

	private static Logger logger = LoggerFactory.getLogger(TypedNgram.class);

	Joiner JOINER_TOKEN_EMPTY = Joiner.on("").skipNulls();
	HashSet<Character> punctuations = Sets.newHashSet(',', '.', ';', '!', '?', '-');
	String REGEX_ALPH = "[a-zA-Z]+";

	private boolean containsPunctuationMiddle(String val) {
		char[] chars = val.toCharArray();
		for (int i = 1; i < val.length() - 1; ++i)
			if (punctuations.contains(chars[i]))
				return true;
		return false;
	}

	private boolean startsPunctuation(String val) {
		if (val.length() > 0 && punctuations.contains(val.charAt(0)))
			return true;
		return false;
	}

	private boolean endsPunctuation(String val) {
		if (val.length() > 0 && punctuations.contains(val.charAt(val.length() - 1)))
			return true;
		return false;
	}

	private static class TraverseInfo {
		public String docid;
		public String ckey;
		public String wkey;
		public String pkey;
	}

	public TypedNgram() {
		this.topk = 2000;
	}

	private int topk = Integer.MAX_VALUE;

	public TypedNgram(int topK) {
		this.topk = topK;
	}

	private Map<String, double[]> docRepresentations = new HashMap<String, double[]>();

	public void traverseTypedCharGrams(Iterable<Document> docs, int n, Consumer<TraverseInfo> consumer) {
		for (Document doc : docs) {
			if (doc == null)
				continue;
			String[] sents = SentenceDetector.sentenceDetectorStandford.detectSentences(doc.rawContent);
			for (String sent : sents) {
				sent = sent.trim();
				if (sent.length() < 1)
					continue;
				LinkedList<Character> queue = new LinkedList<>();
				char[] chars = sent.toCharArray();
				for (int i = 0; i < chars.length; ++i) {
					char chr = chars[i];
					if (queue.size() < n)
						queue.addLast(chr);
					else {
						String key = JOINER_TOKEN_EMPTY.join(queue);
						String ckey = null;
						if (i - n == 0 || chars[i - n - 1] == ' ')
							ckey = "prefix-" + key;
						else if (i == chars.length - 1 || chars[i + 1] == ' ')
							ckey = "suffix-" + key;
						else if (key.startsWith(" "))
							ckey = "space-prefix-" + key;
						else if (key.endsWith(" "))
							ckey = "space-suffix-" + key;

						String wkey = null;
						if ((i == n || chars[i - n - 1] == ' ') && (i == chars.length - 1 || chars[i + 1] == ' '))
							wkey = "whole-word-" + key;
						else if (i - n - 1 >= 0 && chars[i - n - 1] != ' ' && i + 1 < chars.length
								&& chars[i + 1] != ' ' && key.matches(REGEX_ALPH))
							wkey = "mid-word-" + key;
						else if (!key.startsWith(" ") && !key.endsWith(" ") && key.contains(" "))
							wkey = "multi-word-" + key;

						String pkey = null;
						if (startsPunctuation(key) && !containsPunctuationMiddle(key))
							pkey = "beg-punct-" + key;
						else if (endsPunctuation(key) && !containsPunctuationMiddle(key))
							pkey = "end-punct-" + key;
						else if (containsPunctuationMiddle(key))
							pkey = "mid-punct-" + key;

						TraverseInfo info = new TraverseInfo();
						info.docid = doc.id;
						info.ckey = ckey;
						info.wkey = wkey;
						info.pkey = pkey;
						consumer.accept(info);

						queue.removeFirst();
						queue.addLast(chr);
					}
				}
			}
		}
	}

	public void addCharacterNgram(Iterable<Document> docs, Map<String, double[]> docRepresentations, int n) {
		// replace digit
		// remove tag
		// replace url (already done)

		logger.info("building vocab");

		HashMap<String, Integer> counts = new HashMap<>();
		traverseTypedCharGrams(docs, n, info -> {
			if (info.ckey != null)
				counts.compute(info.ckey, (k, v) -> v == null ? 1 : v + 1);
			if (info.wkey != null)
				counts.compute(info.wkey, (k, v) -> v == null ? 1 : v + 1);
			if (info.pkey != null)
				counts.compute(info.pkey, (k, v) -> v == null ? 1 : v + 1);
		});

		logger.info("ranking vocab");

		Ranker<String> top = new Ranker<>(this.topk);
		counts.keySet().stream().forEach(key -> top.push(counts.get(key), key));
		ArrayList<String> cngrams = new ArrayList<>(top.getKeys());
		Map<String, Integer> indMap = IntStream.range(0, cngrams.size())
				.mapToObj(ind -> new EntryPair<>(cngrams.get(ind), ind))
				.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

		logger.info("building features");

		HashMap<String, double[]> newRep = new HashMap<>();
		traverseTypedCharGrams(docs, n, info -> {
			double[] vec = newRep.get(info.docid);
			if (vec == null)
				vec = new double[cngrams.size()];
			if (info.ckey != null)
				if (indMap.containsKey(info.ckey))
					vec[indMap.get(info.ckey)]++;
			if (info.wkey != null)
				if (indMap.containsKey(info.wkey))
					vec[indMap.get(info.wkey)]++;
			if (info.pkey != null)
				if (indMap.containsKey(info.pkey))
					vec[indMap.get(info.pkey)]++;
			newRep.put(info.docid, vec);
		});

		for (Entry<String, double[]> ent : newRep.entrySet()) {
			double[] old = docRepresentations.get(ent.getKey());
			if (old == null)
				old = ent.getValue();
			else
				old = MathUtilities.concate(old, ent.getValue());
			docRepresentations.put(ent.getKey(), old);
		}
	}

	public void train(Iterable<Document> docs) {
		addCharacterNgram(docs, docRepresentations, 3);
	}

	public Map<String, double[]> getDocRepresentations() {
		return this.docRepresentations;
	}

	public static void main(String[] args) {
		String NLP_UTIL_PATH = "E:/authorship-cyb/nlps/";
		NlpUtilsOpennlp.PATH_NLP_MODELS = NLP_UTIL_PATH + "/opennlp/";
		NlpUtilsStandford.PATH_STANDFORD_MODELS = NLP_UTIL_PATH + "/standford/";

		Document document = new Document();
		document.id = "t1";
		document.rawContent = "The actors wanted to see if the pact seemed like an old-fashioned one.";
		TypedNgram ngram = new TypedNgram();
		ngram.train(Arrays.asList(document, document, document, document, document));
		System.out.println(Arrays.toString(ngram.getDocRepresentations().get(document.id)));
	}

}
