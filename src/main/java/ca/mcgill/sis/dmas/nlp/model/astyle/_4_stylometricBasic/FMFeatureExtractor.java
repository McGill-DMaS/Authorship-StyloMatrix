/**
 * @author Benjamin Fung
 * References: 
 * 1/ A Framework for Authorship Identification of Online Messages: Writing-Style Features and Classification Techniques
 * 2/ Mining E-mail Content for Author Identification Forensics
 * honghui modified @2012-9-11 line 96
 */
package ca.mcgill.sis.dmas.nlp.model.astyle._4_stylometricBasic;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.math.linear.OpenMapRealVector;
import org.apache.commons.math.linear.RealVector;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import ca.mcgill.sis.dmas.io.array.DmasArrayOperations;
import ca.mcgill.sis.dmas.io.collection.Counter;
import ca.mcgill.sis.dmas.io.collection.DmasCollectionOperations;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class FMFeatureExtractor {

	public static class SparseVector implements Serializable {
		private static final long serialVersionUID = -3604939356493929642L;
		public HashMap<Integer, Double> vals = new HashMap<>();

		public void fillUpTo(Integer ind, Double val) {
			for (int i = 0; i < ind; i++)
				vals.put(i, val);
		}

		public Double get(Integer ind) {
			if (vals.containsKey(ind))
				return vals.get(ind);
			else
				return 0d;
		}

		public void set(Integer ind, Double val) {
			vals.put(ind, val);
		}

		public void inc(Integer ind) {
			vals.compute(ind, (k, v) -> v == null ? 1 : v + 1);
		}

		public Collection<Double> vals() {
			return vals.values();
		}

		public Set<Integer> inds() {
			return vals.keySet();
		}
	}

	public static final int FM_NUM_ALPHABETS = 26;
	public static final int FM_MAX_WORDLENGTH = 30; // count frequencies of word
													// with maximum length 30.
													// All words >30 are
													// considered 30.

	public static final char[] FM_SPECIAL_CHARS = { '~', '@', '#', '$', '%', '^', '&', '*', '-', '_', '=', '+', '>',
			'<', '[', ']', '{', '}', '/', '\\', '|' };
	public static final char[] FM_SENTENCE_DELIMITERS = { '.', '!', '?' };
	public static final char[] FM_PUNCTUATIONS = { ',', '.', '?', '!', ':', ';', '\'', '\"' };

	public static final int FM_NUMERIC_UPPER_A = (int) 'A';
	public static final int FM_NUMERIC_UPPER_Z = (int) 'Z';
	public static final int FM_NUMERIC_LOWER_A = (int) 'a';
	public static final int FM_NUMERIC_LOWER_Z = (int) 'z';

	public static final String[] FM_FUNCTION_WORDS = { "a", "about", "above", "after", "all", "although", "am", "among",
			"an", "and", "another", "any", "anybody", "anyone", "anything", "are", "around", "as", "at", "be",
			"because", "before", "behind", "below", "beside", "between", "both", "but", "by", "can", "cos", "do",
			"down", "each", "either", "enough", "every", "everybody", "everyone", "everything", "few", "following",
			"for", "from", "have", "he", "her", "him", "i", "if", "in", "including", "inside", "into", "is", "it",
			"its", "latter", "less", "like", "little", "lots", "many", "me", "more", "most", "much", "must", "my",
			"near", "need", "neither", "no", "nobody", "none", "nor", "nothing", "of", "off", "on", "once", "one",
			"onto", "opposite", "or", "our", "outside", "over", "own", "past", "per", "plenty", "plus", "regarding",
			"same", "several", "she", "should", "since", "so", "some", "somebody", "someone", "something", "such",
			"than", "that", "the", "their", "them", "these", "they", "this", "those", "though", "through", "till", "to",
			"toward", "towards", "under", "unless", "unlike", "until", "up", "upon", "us", "used", "via", "we", "what",
			"whatever", "when", "where", "whether", "which", "while", "who", "whoever", "whom", "whose", "will", "with",
			"within", "without", "worth", "would", "yes", "you", "your" };
	public static final String[] FM_GREETINGS = { "hello", "greetings", "good morning", "good afternoon",
			"good evening", "good night", "dear", "hi", "how are you", "sir", "madam" };
	public static final String[] FM_CONTENT_SPECIFIC_WORDS = { "u fat", "u ugly", "cutie gurl", "crack", "keygen",
			"free", "click", "promo", "fraud", "verify", "pin", "pass", "nose candy", "snow", "cock", "snowbird",
			"click", "birthday card", "hurry up", "mines", "bomb", "safety pin", "explosive" };
	public static final String[] FM_GENDER_PREFERENTIAL_ENDING_WITH = { "able", "al", "ful", "ible", "ic", "ive",
			"less", "ly", "ous" };
	public static final String[] FM_GENDER_PREFERENTIAL_SORRY_WORDS = { "sorry", "excuse", "apology", "apologia",
			"sorrow", "regret" };

	private String m_str;
	private Document m_doc;
	private int m_nChars, m_nDigits, m_nLetters, m_nUppers, m_nWhiteSpaces, m_nSpaces, m_nTabs;
	private int m_nWords, m_nShortWords, m_totalWordLength, m_nSentences, m_nLines, m_nBlankLines, m_nParagraphs;
	private int m_nTotalPunctuations, m_nTotalFunctionWords, m_nTotalSorryWords;

	private int[] m_nAlphabets = new int[FM_NUM_ALPHABETS];
	private int[] m_nSpecialChars = new int[FM_SPECIAL_CHARS.length];
	private int[] m_nWordWithLength = new int[FM_MAX_WORDLENGTH];
	private int[] m_nPunctuations = new int[FM_PUNCTUATIONS.length];
	private int[] m_nFunctionWords = new int[FM_FUNCTION_WORDS.length];
	private int[] m_nContentSpecWords = new int[FM_CONTENT_SPECIFIC_WORDS.length];
	private int[] m_nGenderPrefEndWith = new int[FM_GENDER_PREFERENTIAL_ENDING_WITH.length];

	public List<EntryPair<String, Double>> wordList;

	private class WordScore implements Comparable<WordScore> {
		private String word;
		private double score;
		public double weight;

		public WordScore(String word_, double score_, double weight) {
			word = word_;
			score = score_;
			this.weight = weight;
		}

		public int compareTo(WordScore o) {
			if (this.score > o.score) {
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public String toString() {
			return this.word + " " + this.score + " " + this.weight;
		}
	}

	public static List<String> nGramsToken(int n, String[] strs) {
		List<String> ngrams = new ArrayList<String>();
		for (int i = 0; i < strs.length - n + 1; i++)
			ngrams.add(concatToken(strs, i, i + n));
		return ngrams;
	}

	public static String concatToken(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++) {
			if (i > start)
				sb.append("-");
			sb.append(words[i]);
		}
		return sb.toString().trim();
	}

	public static List<String> nGramsCharacter(int n, char[] strs) {
		List<String> ngrams = new ArrayList<String>();
		for (int i = 0; i < strs.length - n + 1; i++)
			ngrams.add(concatCharacter(strs, i, i + n));
		return ngrams;
	}

	public static String concatCharacter(char[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(words[i]);
		return sb.toString().trim();
	}

	public boolean constructWordList(Iterable<Document> docs, int topK, boolean rankedByFrequency) {

		if (wordList != null && wordList.size() > 1)
			return true;

		Counter index = Counter.zero();
		Map<String, Integer> classMap = StreamSupport.stream(docs.spliterator(), false)
				.collect(Collectors.toMap(doc -> doc.id, doc -> {
					Integer val = index.count;
					index.inc();
					return val;
				}));

		if (classMap.size() < 2)
			return false;
		SparseVector classes = new SparseVector();
		classes.fillUpTo(index.getVal(), 1d);
		baseH = H(classes);

		// statistics
		TreeSet<WordScore> sortedSet_lexical = null;
		{
			sortedSet_lexical = new TreeSet<WordScore>();
			Map<String, SparseVector> stats_lexical = this.tokenState(docs, classMap, this::traverseTokenNgram,
					index.count);
			updateV2(stats_lexical, sortedSet_lexical, topK, rankedByFrequency);
		}

		TreeSet<WordScore> sortedSet_character = null;
		{
			sortedSet_character = new TreeSet<WordScore>();
			Map<String, SparseVector> stats_character = this.tokenState(docs, classMap, this::traverseCharacterNgram,
					index.count);
			updateV2(stats_character, sortedSet_character, topK, rankedByFrequency);
		}

		TreeSet<WordScore> sortedSet_pos = null;
		{
			sortedSet_pos = new TreeSet<WordScore>();
			Map<String, SparseVector> stats_pos = this.tokenState(docs, classMap, this::traversePosNgram, index.count);
			updateV2(stats_pos, sortedSet_pos, topK, rankedByFrequency);
		}

		wordList = Stream
				.concat(Stream.concat(sortedSet_lexical.stream(), sortedSet_character.stream()), sortedSet_pos.stream())
				.map(word -> new EntryPair<String, Double>(word.word, word.weight)).collect(Collectors.toList());

		return true;
	}

	public void updateV2(Map<String, SparseVector> stats_lexical, TreeSet<WordScore> sortedSet, int topK,
			boolean rankedByFrequency) {
		// rank
		for (Entry<String, SparseVector> wordEntry : stats_lexical.entrySet()) {
			WordScore ws;
			if (!rankedByFrequency)
				ws = new WordScore(wordEntry.getKey(), infoGain(wordEntry.getValue()),
						invDocFreqency(wordEntry.getValue()));
			else
				ws = new WordScore(wordEntry.getKey(), frequency(wordEntry.getValue()),
						invDocFreqency(wordEntry.getValue()));

			sortedSet.add(ws);
			if (sortedSet.size() > topK)
				sortedSet.pollFirst();
		}
	}

	public volatile boolean inMem = true;
	public volatile boolean posOnly = false;

	private Map<String, SparseVector> createMap() {
		if (inMem)
			return new HashMap<>();
		else
			return DBMaker.tempTreeMap();
	}

	void traverseTokenNgram(Document doc, Consumer<String> consumer) {
		for (Sentence[] sentenceTagged : doc.taggedSentences()) {
			Sentence sentence = sentenceTagged[0];
			// word gram
			for (int j = 1; j < 4; ++j) {
				for (String gram : nGramsToken(j, sentence.tokens)) {
					String pgram = "W:" + gram;
					consumer.accept(pgram);
				}
			}
		}
	}

	void traverseCharacterNgram(Document doc, Consumer<String> consumer) {
		for (Sentence[] sentenceTagged : doc.taggedSentences()) {
			Sentence sentence = sentenceTagged[0];
			// word gram
			for (String gram : sentence.tokens) {
				for (int j = 1; j < 4; ++j) {
					for (String chgram : nGramsCharacter(j, gram.toCharArray())) {
						String pgram = "C:" + chgram;
						consumer.accept(pgram);
					}
				}
			}
		}
	}

	void traversePosNgram(Document doc, Consumer<String> consumer) {
		for (Sentence[] sentenceTagged : doc.taggedSentences()) {
			Sentence tags = sentenceTagged[1];
			// word gram
			for (int j = 1; j < 4; ++j) {
				for (String gram : nGramsToken(j, tags.tokens)) {
					String pgram = "P:" + gram;
					consumer.accept(pgram);
				}
			}
		}
	}

	Map<String, SparseVector> tokenState(Iterable<Document> docs, Map<String, Integer> classMap,
			BiConsumer<Document, Consumer<String>> traverser, int dim) {
		Map<String, SparseVector> tokenStats = createMap();
		long size = DmasCollectionOperations.count(docs);
		Counter count = Counter.zero();
		int gate = 0;

		for (Document document : docs) {
			count.inc();
			if (count.getVal() * 100.0 / size > gate) {
				gate++;
				System.out
						.println((count.getVal() * 100.0 / size) + "% " + size + " unique tkns: " + tokenStats.size());
			}
			int ind = classMap.get(document.id);
			Consumer<String> consumer = pgram -> {
				SparseVector values = tokenStats.get(pgram);
				if (values == null) {
					values = new SparseVector();
					tokenStats.put(pgram, values);
				}
				values.inc(ind);
			};
			traverser.accept(document, consumer);
		}
		return tokenStats;
	}

	double frequency(RealVector values) {
		double sum = 0;
		for (int i = 0; i < values.getDimension(); ++i)
			sum += values.getEntry(i);
		return sum;
	}

	double frequency(SparseVector values) {
		return values.vals().stream().mapToDouble(val -> val).sum();
	}

	double frequency(double[] values) {
		double sum = 0;
		for (int i = 0; i < values.length; ++i)
			sum += values[i];
		return sum;
	}

	double invDocFreqency(RealVector values) {
		double sum = 0;
		for (int i = 0; i < values.getDimension(); ++i)
			if (values.getEntry(i) >= 1)
				sum += 1;
		sum = 1.0 / sum;
		if (Double.isNaN(sum))
			return 0.0;
		return sum;
	}

	double invDocFreqency(SparseVector values) {
		double sum = values.inds().size();
		sum = 1.0 / sum;
		if (Double.isNaN(sum))
			return 0.0;
		return sum;
	}

	double invDocFreqency(double[] values) {
		double sum = 0;
		for (int i = 0; i < values.length; ++i)
			if (values[i] >= 1)
				sum += 1;
		sum = 1.0 / sum;
		if (Double.isNaN(sum))
			return 0.0;
		return sum;
	}

	private static double baseH = 0.0;

	double infoGain(RealVector sparseVector) {
		return baseH - H(sparseVector);
	}

	double infoGain(SparseVector sparseVector) {
		return baseH - H(sparseVector);
	}

	double infoGain(double[] values) {
		return baseH - H(values);
	}

	private static double LOG2 = Math.log(2);

	double H(double[] sparseVector) {
		double sum = 0;
		double info = 0.0;
		for (int i = 0; i < sparseVector.length; ++i)
			sum += sparseVector[i];
		for (int i = 0; i < sparseVector.length; ++i) {
			double nvalue = sparseVector[i] / sum;
			if (nvalue != 0)
				info += nvalue * Math.log(nvalue) / LOG2;
		}
		info *= -1;
		if (Double.isNaN(info))
			return 0.0;
		else
			return info;
	}

	double H(SparseVector sparseVector) {
		double sum = 0;
		double info = 0.0;
		sum = sparseVector.vals().stream().mapToDouble(val -> val).sum();
		for (Double val : sparseVector.vals()) {
			double nvalue = val / sum;
			if (nvalue != 0)
				info += nvalue * Math.log(nvalue) / LOG2;
		}
		info *= -1;
		if (Double.isNaN(info))
			return 0.0;
		else
			return info;
	}

	double H(RealVector values) {
		double sum = 0;
		double info = 0.0;
		for (int i = 0; i < values.getDimension(); ++i)
			sum += values.getEntry(i);
		for (int i = 0; i < values.getDimension(); ++i) {
			double nvalue = values.getEntry(i) / sum;
			if (nvalue != 0)
				info += nvalue * Math.log(nvalue) / LOG2;
		}
		info *= -1;
		if (Double.isNaN(info))
			return 0.0;
		else
			return info;
	}

	private HashMap<String, Integer> ngramCounter = new HashMap<String, Integer>();

	int wordCount(String word) {
		Integer count = ngramCounter.get(word);
		if (count == null)
			return 0;
		else
			return count;
	}

	void calculateNGramFeature(Document doc) {
		ngramCounter.clear();

		Consumer<String> consumer = pgram -> {
			ngramCounter.compute(pgram, (k, v) -> v == null ? 1 : v + 1);
		};
		this.traverseTokenNgram(doc, consumer);
		this.traverseCharacterNgram(doc, consumer);
		this.traversePosNgram(doc, consumer);
	}

	// END OF Steven editing

	private HashMap<String, Integer> m_distinctWordCountMap = new HashMap<String, Integer>();
	private HashMap<Integer, Integer> m_distinctWordWithOccurrencesCountMap = new HashMap<Integer, Integer>();

	public FMFeatureExtractor() {
	}

	private void initialize() {
		m_str = "";
		m_doc = null;
		m_nChars = m_nDigits = m_nLetters = m_nUppers = m_nWhiteSpaces = m_nSpaces = m_nTabs = 0;
		m_nWords = m_nShortWords = m_totalWordLength = m_nSentences = m_nLines = 0;
		m_nTotalPunctuations = m_nTotalFunctionWords = m_nTotalSorryWords = 0;
		m_nBlankLines = m_nParagraphs = -1;
		for (int i = 0; i < m_nAlphabets.length; ++i)
			m_nAlphabets[i] = 0;
		for (int i = 0; i < m_nSpecialChars.length; ++i)
			m_nSpecialChars[i] = 0;
		for (int i = 0; i < m_nWordWithLength.length; ++i)
			m_nWordWithLength[i] = 0;
		for (int i = 0; i < m_nPunctuations.length; ++i)
			m_nPunctuations[i] = 0;
		for (int i = 0; i < m_nFunctionWords.length; ++i)
			m_nFunctionWords[i] = 0;
		for (int i = 0; i < m_nContentSpecWords.length; ++i)
			m_nContentSpecWords[i] = 0;
		for (int i = 0; i < m_nGenderPrefEndWith.length; ++i)
			m_nGenderPrefEndWith[i] = 0;

		m_distinctWordCountMap.clear();
		m_distinctWordWithOccurrencesCountMap.clear();
	}

	public boolean loadContent(Document content) {
		initialize();
		m_str = content.rawContent;
		m_doc = content;
		return true;
	}

	// ***********************************
	// Character-based lexical features *
	// ***********************************

	public int getNChars() {
		return m_nChars;
	}

	public double getNAlphabetsPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nLetters / m_nChars;
	}

	public double getNUppersPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nUppers / m_nChars;
	}

	public double getNDigitsPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nDigits / m_nChars;
	}

	public double getNWhiteSpacesPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nWhiteSpaces / m_nChars;
	}

	public double getNSpacesPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nSpaces / m_nChars;
	}

	public double getNSpacesPerWhiteSpace() {
		if (m_nWhiteSpaces == 0)
			return 0;
		else
			return (double) m_nSpaces / m_nWhiteSpaces;
	}

	public double getNTabsPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nTabs / m_nChars;
	}

	public int getNAlphabets(int idx) {
		return m_nAlphabets[idx];
	}

	public int getNSpecialChars(int idx) {
		return m_nSpecialChars[idx];
	}

	// ******************************
	// Word-based lexical features *
	// ******************************

	public int getNWords() {
		return m_nWords;
	}

	public double getNShortWordsPerWord() {
		if (m_nWords == 0)
			return 0;
		else
			return (double) m_nShortWords / m_nWords;
	}

	public double getNWordCharsPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_totalWordLength / m_nChars;
	}

	public double getAvgWordsLength() {
		if (m_nWords == 0)
			return 0;
		else
			return (double) m_totalWordLength / m_nWords;
	}

	public double getAvgCharsPerSentence() {
		if (m_nSentences == 0)
			return 0;
		else
			return (double) m_nChars / m_nSentences;
	}

	public double getAvgWordsPerSentence() {
		if (m_nSentences == 0)
			return 0;
		else
			return (double) m_nWords / m_nSentences;
	}

	public double getNDistinctWordsPerWord() {
		if (m_nWords == 0)
			return 0;
		else
			return (double) m_distinctWordCountMap.size() / m_nWords;
	}

	public int getNHapaxLegomena() {
		if (m_distinctWordWithOccurrencesCountMap.containsKey(1))
			return m_distinctWordWithOccurrencesCountMap.get(new Integer(1)).intValue();
		return 0;
	}

	public double getNHapaxLegomenaPerWord() {
		if (m_nWords == 0)
			return 0;
		else if (m_distinctWordWithOccurrencesCountMap.containsKey(1))
			return m_distinctWordWithOccurrencesCountMap.get(new Integer(1)).doubleValue() / m_nWords;
		return 0;
	}

	public double getNHapaxLegomenaPerDistincWord() {
		if (m_distinctWordCountMap.size() == 0 || !m_distinctWordWithOccurrencesCountMap.containsKey(new Integer(1)))
			return 0;
		else
			return m_distinctWordWithOccurrencesCountMap.get(new Integer(1)).doubleValue()
					/ m_distinctWordCountMap.size();
	}

	public int getNHapaxDislegomena() {
		if (!m_distinctWordWithOccurrencesCountMap.containsKey(new Integer(2)))
			return 0;
		else
			return m_distinctWordWithOccurrencesCountMap.get(new Integer(2)).intValue();
	}

	/**
	 * Yule's K
	 * 
	 * @return (may return Double.NaN)
	 */
	public double getYuleK() {
		double sum = m_distinctWordWithOccurrencesCountMap.entrySet().stream()
				.mapToDouble(ent -> ent.getValue().intValue() * Math.pow(ent.getKey().doubleValue() / m_nWords, 2))
				.sum();
		return 10000 * (-1.0 / m_nWords + sum);
	}

	/**
	 * Simpson's D
	 * 
	 * @return (may return Double.NaN)
	 */
	public double getSimpsonD() {
		return m_distinctWordWithOccurrencesCountMap.entrySet().stream().mapToDouble(ent -> ent.getValue()
				* ((double) ent.getKey() / m_nWords) * ((double) (ent.getKey() - 1) / (m_nWords - 1))).sum();
	}

	/**
	 * Sichel's S
	 */
	public double getSichelS() {
		return (double) getNHapaxDislegomena() / m_distinctWordCountMap.size();
	}

	/**
	 * Brunet's W
	 * 
	 * @return (may return Double.NaN)
	 */
	public double getBrunetW() {
		return Math.pow(m_nWords, Math.pow(m_distinctWordCountMap.size(), 0.172));
	}

	/**
	 * Honore's R (or H in the paper)
	 * 
	 * @return (may return Double.NaN)
	 */
	public double getHonoreR() {
		return (100 * Math.log10(m_nWords)) / (1 - (double) getNHapaxLegomena() / m_distinctWordCountMap.size());
	}

	public double getNWordsWithLengthPerWord(int idx) {
		if (m_nWords == 0)
			return 0;
		else
			return (double) m_nWordWithLength[idx] / m_nWords;
	}

	/*
	 * //****************************************************** // 96.G U I R A
	 * U D's R //****************************************************** public
	 * double guiraudsR(String content){ int types = getTypes2(content);
	 * StringTokenizer st = new StringTokenizer(content); int tokens =
	 * st.countTokens();
	 * 
	 * if (tokens == 0) return 0.0; else return (double) types /
	 * Math.sqrt(tokens); }
	 * 
	 * //*********************************************** // 97.H E R D A N's C
	 * //*********************************************** public double
	 * herdansC(String content){ int types = getTypes2(content); StringTokenizer
	 * st = new StringTokenizer(content); int tokens = st.countTokens();
	 * 
	 * if (types == 0 || tokens == 0 || tokens == 1) return 0.0; else return
	 * (double) Math.log10(types) / Math.log10(tokens); }
	 * 
	 * //*********************************************** // 98.H E R D A N's V
	 * //*********************************************** public double
	 * herdansV(String content){ double C = herdansC(content); int W =
	 * wordCount(content);
	 * 
	 * return (double)Math.pow(W, C); }
	 * 
	 * //*********************************************** // 99.Rubet's K
	 * //*********************************************** public double
	 * rubetsK(String content){ double V =
	 * (double)Math.log10(getTypes2(content))
	 * /(Math.log10(Math.log10(wordCount(content)))); return 0; }
	 * 
	 * //*********************************************** // 100.Maas's A
	 * //*********************************************** public double
	 * massA(String content){ double a = (double)
	 * (Math.log10(wordCount(content)) - Math.log10(getTypes2(content))) /
	 * Math.pow(Math.log10(wordCount(content)), 2) ; return a; }
	 * 
	 * 
	 * //*********************************************** // 101.Dugast's U
	 * //*********************************************** public double
	 * dugastU(String content){ double a = 1/massA(content); return a; }
	 * 
	 * //*********************************************** // 102.Lukjanenkov &
	 * Neistoj //*********************************************** public double
	 * lucNei(String content){ double LN = (double) ( 1 -
	 * Math.pow(getTypes2(content), 2)) / Math.pow(getTypes2(content), 2) *
	 * Math.log10(wordCount(content)) ; return LN; }
	 * 
	 * //--------------------------------------------------- // U B E R
	 * //--------------------------------------------------- public double
	 * uber(String content){ double result = 0.0; int types=getTypes(content,
	 * 1).size(); StringTokenizer st = new StringTokenizer(content); int tokens
	 * = st.countTokens();
	 * 
	 * if (tokens == 0 || types == 0) return 0.0; else result = (double)
	 * (Math.pow((Math.log10(tokens)),
	 * 2))/(Math.log10(tokens)-Math.log10(types));
	 * 
	 * if(Double.isNaN(result)) return 0.0; else return result; }
	 */

	// ********************
	// Syntatic features *
	// ********************

	public int getNPunctuations(int idx) {
		return m_nPunctuations[idx];
	}

	public double getNPunctuationsPerChar() {
		if (m_nChars == 0)
			return 0;
		else
			return (double) m_nTotalPunctuations / m_nChars;
	}

	public int getNFunctionWords(int idx) {
		return m_nFunctionWords[idx];
	}

	public double getNFunctionWordsPerWord() {
		if (m_nWords == 0)
			return 0;
		else
			return (double) m_nTotalFunctionWords / m_nWords;
	}

	// **********************
	// Structural features *
	// **********************
	// Regular expression reference: http://regexlib.com/

	public int getNLines() {
		return m_nLines;
	}

	public int getNSentences() {
		return m_nSentences;
	}

	public int getNParagraphs() {
		if (m_nParagraphs != -1) // avoid counting again
			return m_nParagraphs;

		// (\r\n\r\n|\n\n)+ by Kasra
		Pattern pattern = Pattern.compile("(\r?\n){2,}.|(\r?\n)+\t.", Pattern.DOTALL);
		m_nParagraphs = pattern.matcher(m_str).groupCount() + 1;
		return m_nParagraphs;
	}

	public double getNSentencesPerParagraph() {
		if (getNParagraphs() == 0)
			return 0;
		return (double) m_nSentences / getNParagraphs();
	}

	public double getNCharsPerParagraph() {
		if (getNParagraphs() == 0)
			return 0;
		return (double) m_nChars / getNParagraphs();
	}

	public double getNWordsPerParagraph() {
		if (getNParagraphs() == 0)
			return 0;
		return (double) m_nWords / getNParagraphs();
	}

	/*
	 * public double getNBlankLinesPerLine() { if (m_nBlankLines != -1) // avoid
	 * counting again return (double) m_nBlankLines / m_nLines;
	 * 
	 * Pattern pattern = Pattern.compile("(\r\n\r\n|\n\n)+", Pattern.DOTALL);
	 * m_nBlankLines = pattern.matcher(m_str).groupCount(); return (double)
	 * m_nBlankLines / m_nLines; }
	 */

	public boolean hasGreeting() {
		String lowerStr = m_str.toLowerCase();
		for (String greetingStr : FM_GREETINGS) {
			int idx = lowerStr.indexOf(greetingStr.toLowerCase());
			if (idx > -1 && idx < 20)
				return true;
		}
		return false;
	}

	public boolean hasBlankLineBetweenParagraphs() {
		Pattern pattern = Pattern.compile("(\r?\n){2,}.", Pattern.DOTALL);
		return pattern.matcher(m_str).matches();
	}

	public boolean hasIndentBeforeParagraphs() {
		Pattern pattern = Pattern.compile("(\r?\n)+\t.", Pattern.DOTALL);
		return pattern.matcher(m_str).matches();
	}

	public boolean hasEmailInSignature() {
		// [A-Za-z0-9_\\-\\.]+\\@[A-Za-z0-9_]+\\.[A-Za-z]+
		// ^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$
		Pattern pattern = Pattern.compile(
				"^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$");
		return pattern.matcher(m_str).matches();
	}

	public boolean hasTelInSignature() {
		// String patternStr = "(\\d-)?(\\d{3}-)?\\d{3}-\\d{4}";
		// ^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$
		Pattern pattern = Pattern
				.compile("^([0-9]( |-)?)?(\\(?[0-9]{3}\\)?|[0-9]{3})( |-)?([0-9]{3}( |-)?[0-9]{4}|[a-zA-Z0-9]{7})$");
		return pattern.matcher(m_str).matches();
	}

	public boolean hasURLInSignature() {
		// String patternStr =
		// "[a-z]+\\:\\/\\/[a-zA-Z0-9\\-\\_\\~\\}\\\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+(\\.[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+)+(\\/[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+)+(\\.[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+)(\\?[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+\\=[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+(\\&[a-zA-Z0-9]+\\=[a-zA-Z0-9\\-\\_\\~\\}\\{\'\"\\,\\@\\#\\%\\(\\)\\-\\+\\=\\<\\>]+)*)*";
		// (http|ftp|https):\/\/[\w\-_]+(\.[\w\-_]+)+([\w\-\.,@?^=%&amp;:/~\+#]*[\w\-\@?^=%&amp;/~\+#])?
		Pattern pattern = Pattern.compile(
				"(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
		return pattern.matcher(m_str).matches();
	}

	// ****************************
	// Content-specific features *
	// ****************************

	public int getNContentSpecificWords(int idx) {
		return m_nContentSpecWords[idx];
	}

	// *******************************
	// Gender-preferential features *
	// *******************************

	public double getNWordsEndingWith(int idx) {
		return (double) m_nGenderPrefEndWith[idx] / m_nWords;
	}

	public int getNSorryWords() {
		return m_nTotalSorryWords;
	}

	// ***********************
	// Compute the features *
	// ***********************

	/**
	 * Character-based features
	 */
	public boolean computeCharBasedFeatures() {
		for (int i = 0; i < m_str.length(); i++) {
			char theChar = m_str.charAt(i);
			if (theChar != ' ' && theChar != '\t' && theChar != '\n' && theChar != '\r')
				m_nChars++;

			if (Character.isLetter(theChar)) {
				++m_nLetters;

				// count alphabets
				int numericValue = (int) theChar;
				if (Character.isUpperCase(theChar)) {
					++m_nUppers;
					if (numericValue >= FM_NUMERIC_UPPER_A && numericValue <= FM_NUMERIC_UPPER_Z)
						++m_nAlphabets[numericValue - FM_NUMERIC_UPPER_A];
				} else if (Character.isLowerCase(theChar)) {
					if (numericValue >= FM_NUMERIC_LOWER_A && numericValue <= FM_NUMERIC_LOWER_Z)
						++m_nAlphabets[numericValue - FM_NUMERIC_LOWER_A];
				}
			} else if (Character.isDigit(theChar))
				++m_nDigits;
			else if (Character.isWhitespace(theChar))
				++m_nWhiteSpaces;
			else {
				// count special characters
				for (int s = 0; s < FM_SPECIAL_CHARS.length; ++s) {
					if (theChar == FM_SPECIAL_CHARS[s])
						++m_nSpecialChars[s];
				}

				// count number of sentences
				for (int senIdx = 0; senIdx < FM_SENTENCE_DELIMITERS.length; ++senIdx)
					if (theChar == FM_SENTENCE_DELIMITERS[senIdx])
						++m_nSentences;

				// count punctuations
				for (int punIdx = 0; punIdx < FM_PUNCTUATIONS.length; ++punIdx) {
					if (theChar == FM_PUNCTUATIONS[punIdx]) {
						++m_nPunctuations[punIdx];
						++m_nTotalPunctuations;
					}
				}
			}

			if (theChar == ' ')
				++m_nSpaces;
			else if (theChar == '\t')
				++m_nTabs;
			else if (theChar == '\n')
				++m_nLines;
		}

		if (m_nChars != 0 && m_nSentences == 0)
			m_nSentences = 1; // in case the entire message has no sentence
								// delimiter, consider it as a single sentence.

		return true;
	}

	/**
	 * Word-based features
	 */
	public boolean computeWordBasedFeatures() {
		String theToken;
		StringTokenizer strTokens = new StringTokenizer(m_str);
		while (strTokens.hasMoreTokens()) {
			theToken = strTokens.nextToken().trim();
			if (theToken.isEmpty())
				continue;

			// count number of words
			++m_nWords;

			// count total number of characters in words
			m_totalWordLength += theToken.length();

			// count short words
			if (theToken.length() < 4)
				++m_nShortWords;

			// count frequency of distinct word
			String lowerToken = theToken.toLowerCase();
			if (m_distinctWordCountMap.containsKey(lowerToken)) {
				int oldValue = m_distinctWordCountMap.get(lowerToken).intValue();
				m_distinctWordCountMap.put(lowerToken, new Integer(oldValue + 1));
			} else
				m_distinctWordCountMap.put(lowerToken, new Integer(1));

			// count word length frequency
			if (theToken.length() <= FM_MAX_WORDLENGTH)
				++m_nWordWithLength[theToken.length() - 1];
			else
				++m_nWordWithLength[FM_MAX_WORDLENGTH - 1];

			// count function word frequency
			for (int fIdx = 0; fIdx < FM_FUNCTION_WORDS.length; ++fIdx) {
				if (theToken.equalsIgnoreCase(FM_FUNCTION_WORDS[fIdx])) {
					++m_nFunctionWords[fIdx];
					++m_nTotalFunctionWords;
				}
			}

			// count content specific word frequency
			for (int cIdx = 0; cIdx < FM_CONTENT_SPECIFIC_WORDS.length; ++cIdx) {
				if (theToken.equalsIgnoreCase(FM_CONTENT_SPECIFIC_WORDS[cIdx]))
					++m_nContentSpecWords[cIdx];
			}

			// count gender specific word frequency
			for (int gIdx = 0; gIdx < FM_GENDER_PREFERENTIAL_ENDING_WITH.length; ++gIdx) {
				if (lowerToken.endsWith(FM_GENDER_PREFERENTIAL_ENDING_WITH[gIdx].toLowerCase()))
					++m_nGenderPrefEndWith[gIdx];
			}

			// count gender specific frequency
			for (int sIdx = 0; sIdx < FM_GENDER_PREFERENTIAL_SORRY_WORDS.length; ++sIdx) {
				if (theToken.equalsIgnoreCase(FM_GENDER_PREFERENTIAL_SORRY_WORDS[sIdx]))
					++m_nTotalSorryWords;
			}
		}

		// Count number of words occurring n times.
		Collection<Integer> distinctWordCounts = m_distinctWordCountMap.values();
		for (Object distinctWordCount : distinctWordCounts) {
			if (m_distinctWordWithOccurrencesCountMap.containsKey((Integer) distinctWordCount)) {
				int oldWordCount = m_distinctWordWithOccurrencesCountMap.get((Integer) distinctWordCount).intValue();
				m_distinctWordWithOccurrencesCountMap.put((Integer) distinctWordCount, new Integer(oldWordCount + 1));
			} else
				m_distinctWordWithOccurrencesCountMap.put((Integer) distinctWordCount, new Integer(1));
		}
		return true;
	}

}
