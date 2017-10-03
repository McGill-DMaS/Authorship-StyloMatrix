package ca.mcgill.sis.dmas.nlp.model.astyle._2_character;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import ca.mcgill.sis.dmas.io.collection.DmasCollectionOperations;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.EntryTriplet;
import ca.mcgill.sis.dmas.io.collection.IteratorSafeGen;
import ca.mcgill.sis.dmas.io.collection.Pool;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.GradientProgress;
import ca.mcgill.sis.dmas.nlp.model.astyle.NodeWord;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;
import ca.mcgill.sis.dmas.nlp.model.astyle.RandL;
import ca.mcgill.sis.dmas.nlp.model.astyle.WordEmbedding;

import static ca.mcgill.sis.dmas.io.collection.DmasCollectionOperations.zip;

import static ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;

import static java.lang.Math.sqrt;

public class LearnerChar2VecEmbedding {

	private static Logger logger = LoggerFactory.getLogger(LearnerChar2VecEmbedding.class);

	public static class C2VParam extends Param {
		private static final long serialVersionUID = -817341338942724187L;
		public int min_freq = 3;
		public int vec_dim = 200;
		public double optm_subsampling = 1e-4;
		public double optm_char_subsampling = 1e-4;
		public double optm_initAlpha = 0.05;
		public int optm_negSample = 25;
		public int optm_parallelism = 1;
		public int optm_iteration = 20;
		public int optm_aphaUpdateInterval = 10000;
		public int optm_ngram = 2;

	}

	public Consumer<Integer> iterationHood = null;

	public Map<String, NodeWord> vocab = null;
	public Map<String, NodeWord> vocabChar = null;
	public Map<String, NodeWord> trainCharPreMap = null;
	public List<NodeWord> vocabL = null;
	public List<NodeWord> vocabLC = null;
	public int[] pTable;
	public int[] pcTable;
	public transient volatile double alpha;
	public transient volatile long tknCurrent;
	public transient volatile long tknLastUpdate;
	public volatile long tknTotal;
	public volatile boolean debug = true;
	public C2VParam param;

	private void preprocess(Iterable<Document> documents) {

		vocab = null;

		// frequency map:
		final HashMap<String, Long> counter = new HashMap<>();
		documents.forEach(doc -> doc.forEach(sent -> sent
				.forEach(token -> counter.compute(token.trim().toLowerCase(), (w, c) -> c == null ? 1 : c + 1))));

		// add additional word (for /n)
		counter.put("</s>", Long.MAX_VALUE);

		// create word nodes (matrix)
		vocab = counter.entrySet().stream().parallel().filter(en -> en.getValue() >= param.min_freq)
				.collect(toMap(Map.Entry::getKey, p -> new NodeWord(p.getKey(), p.getValue())));

		// total valid word count
		tknTotal = vocab.values().stream().filter(w -> !w.token.equals("</s>")).mapToLong(w -> w.freq).sum();

		// vocabulary list (sorted)
		vocabL = vocab.values().stream().sorted((a, b) -> b.token.compareTo(a.token))
				.sorted((a, b) -> Double.compare(b.freq, a.freq)).collect(toList());

		// reset frequency for /n
		vocabL.get(0).freq = 0;

		// initialize matrix:
		RandL rd = new RandL(1);
		vocabL.stream().forEach(node -> node.initOutLayer(param.vec_dim));

		// sub-sampling probability
		if (param.optm_subsampling > 0) {
			double fcount = param.optm_subsampling * tknTotal;
			vocabL.stream().parallel().forEach(w -> w.samProb = (sqrt(w.freq / fcount) + 1) * fcount / w.freq);
		}

		pTable = createPTbl(vocabL, (int) 1e8, 0.75);

		// char gram table:
		vocabChar = new HashMap<>();
		vocabL.stream()
				.forEach(word -> charNgramCf2(word.token, param.optm_ngram)//
						.stream()//
						.map(String::toLowerCase)//
						.forEach(chr -> vocabChar.compute(chr, (k, v) -> {
							if (v == null)
								return new NodeWord(chr, word.freq);
							v.freq += word.freq;
							return v;
						})));
		// sorted char gram list:
		vocabLC = vocabChar.values().stream().sorted((a, b) -> b.token.compareTo(a.token))
				.sorted((a, b) -> Double.compare(b.freq, a.freq)).collect(toList());

		// init in-out layer:
		vocabLC.stream().forEach(node -> node.init(param.vec_dim, rd));
		long tknCharTotal = vocabLC.stream().mapToLong(chrNode -> chrNode.freq).count();

		// sub-sampling probability
		if (param.optm_char_subsampling > 0) {
			double fcount = param.optm_subsampling * tknCharTotal;
			vocabLC.stream().parallel().forEach(w -> w.samProb = (sqrt(w.freq / fcount) + 1) * fcount / w.freq);
		}

		// negative-sampling probability:
		pcTable = createPTbl(vocabLC, (int) 1e8, 0.75);

		// train doc map:
		trainCharPreMap = new HashMap<>();
		documents.forEach(doc -> trainCharPreMap.put(doc.id, new NodeWord(doc.id, 1)));
		trainCharPreMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

		if (debug)
			logger.info("Vocab {}; Total char vocab:{}; Total words {};", vocabL.size(), vocabChar.size(), tknTotal);

	}

	private void gradientDecend(final Iterable<Document> documents, Map<String, NodeWord> charPreMap, long numTkns,
			int alphaUpdateInterval) throws InterruptedException, ExecutionException {
		tknLastUpdate = 0;
		tknCurrent = 0;
		// training
		GradientProgress p = new GradientProgress(numTkns * param.optm_iteration);
		// if (debug)
		p.start(logger);

		// thread-safe batch consumer generator:
		final IteratorSafeGen<Document> gen = new IteratorSafeGen<>(documents, 100, param.optm_iteration,
				this.iterationHood);

		new Pool(param.optm_parallelism).start(indx -> {
			RandL rl = new RandL(indx);
			double[] bfIn = new double[param.vec_dim], bfNeul1e = new double[param.vec_dim];
			gen.subIterable().forEach(doc -> {
				for (Sentence sent : doc) {

					// update alpha:
					if (param.optm_aphaUpdateInterval > 0
							&& tknCurrent - tknLastUpdate > param.optm_aphaUpdateInterval) {
						alpha = param.optm_initAlpha * (1.0 - 1.0 * tknCurrent / (numTkns * param.optm_iteration + 1));
						alpha = alpha < param.optm_initAlpha * 0.0001 ? param.optm_initAlpha * 0.0001 : alpha;
						if (debug)
							p.report(logger, tknCurrent, alpha);
						else
							p.reportIfProgressSatisfied(logger, tknCurrent, alpha, val -> {
								// int progress = (int) (val * 10);
								// if (progress % 10 == 0)
								// return true;
								// else
								// return false;
								return false;
							});
						tknLastUpdate = tknCurrent;
					}

					// dictionary lookup & sub-sampling
					List<NodeWord> nsent = Arrays.stream(sent.tokens)
							//
							.map(tkn -> vocab.get(tkn.trim().toLowerCase()))//
							.filter(notNull)//
							.peek(node -> tknCurrent++)//
							.filter(n -> n.samProb >= rl.nextF()) //
							.collect(toList());

					if (!charPreMap.containsKey(doc.id)) {
						logger.error("Critical error. Doc node not found {}", doc);
						return;
					}

					iterate(nsent, charPreMap.get(doc.id), rl, bfIn, bfNeul1e);
				}
			});
		}).waiteForCompletion();
		// if (debug)
		p.complete(logger);
	}

	// negative sampling
	private void iterate(List<NodeWord> nsent, NodeWord docCharNode, RandL rl, double[] bfIn, double[] bfNeul1e) {

		Supplier<NodeWord> wordNgMethod = () -> vocabL.get(pTable[//
		(int) Long.remainderUnsigned(rl.nextR() >>> 16, pTable.length)//
		]);

		nsent.forEach(word -> {

			List<NodeWord> charGrams = charNgramCf2(word.token, param.optm_ngram)//
					.stream()//
					.map(chr -> vocabChar.get(chr.toLowerCase()))//
					.filter(notNull).filter(n -> n.samProb >= rl.nextF())//
					.collect(Collectors.toList());
			// Set<String> winValid = charGrams.stream().map(node ->
			// node.token).collect(Collectors.toSet());

			for (NodeWord charNode : charGrams) {

				// NodeWord charNode = vocabChar.get(chargram); // character ->
				// word:
				Arrays.fill(bfNeul1e, 0.0);
				Arrays.fill(bfIn, 0.0);
				add(bfIn, charNode.neuIn);
				add(bfIn, docCharNode.neuIn);
				div(bfIn, 2);
				ngSamp(word, bfIn, bfNeul1e, wordNgMethod);
				if (!charNode.fixed)
					add(charNode.neuIn, bfNeul1e);
				if (!docCharNode.fixed)
					add(docCharNode.neuIn, bfNeul1e);
			}

			// Supplier<NodeWord> charNgMethod = () -> {
			// NodeWord sample = null;
			// do {
			// sample = vocabLC.get(pcTable[//
			// (int) Long.remainderUnsigned(rl.nextR() >>> 16, pcTable.length)//
			// ]);
			// } while (winValid.contains(sample.token));
			// return sample;
			// };
			//
			// for (NodeWord node1 : charGrams)
			// for (NodeWord node2 : charGrams)
			// if (node1 != node2) {
			// Arrays.fill(bfNeul1e, 0);
			// Arrays.fill(bfIn, 0);
			// add(bfIn, node1.neuIn);
			// add(bfIn, docCharNode.neuIn);
			// div(bfIn, 2);
			// ngSamp(node2, bfIn, bfNeul1e, charNgMethod);
			// // softMax(node2, bfIn, vocabChar.values(), bfNeul1e);
			// if (!node1.fixed)
			// add(node1.neuIn, bfNeul1e);
			// if (!docCharNode.fixed)
			// add(docCharNode.neuIn, bfNeul1e);
			// }
		});
	}

	private void softMax(NodeWord tar, double[] in, Collection<NodeWord> candidates, double[] neul1e) {
		List<EntryPair<NodeWord, Double>> forward = candidates//
				.stream()//
				.map(node -> new EntryPair<>(node, Math.exp(dot(in, node.neuOut))))//
				.collect(Collectors.toList());
		double sum = forward.stream().mapToDouble(ent -> ent.value).sum();
		forward.stream().forEach(ent -> ent.value /= sum);
		forward.stream().forEach(ent -> {
			double g = 0;
			if (ent.key == tar)
				g = (1 - ent.value) * alpha;
			else
				g = (0 - ent.value) * alpha;
			dxpay(neul1e, ent.key.neuOut, ent.value);
			if (!ent.key.fixed)
				dxpay(ent.key.neuOut, in, g);
		});
		sub(neul1e, tar.neuIn);
		mlp(neul1e, -1 * alpha);
	}

	private void ngSamp(NodeWord tar, double[] in, double[] neul1e, Supplier<NodeWord> ngMethod) {
		for (int i = 0; i < param.optm_negSample + 1; ++i) {
			double label;
			NodeWord nodeOut;
			// NodeWord target;
			if (i == 0) {
				label = 1;
				nodeOut = tar;
			} else {
				label = 0;
				NodeWord rtar = ngMethod.get();
				if (rtar == tar)
					continue;
				nodeOut = rtar;
			}
			double f = exp(dot(in, nodeOut.neuOut));
			double g = (label - f) * alpha;
			dxpay(neul1e, nodeOut.neuOut, g);
			if (!nodeOut.fixed)
				dxpay(nodeOut.neuOut, in, g);
		}
	}

	public void train(Iterable<Document> docs) throws InterruptedException, ExecutionException {
		alpha = param.optm_initAlpha;
		preprocess(docs);
		gradientDecend(docs, trainCharPreMap, tknTotal, param.optm_aphaUpdateInterval);
		fixTrainedModel();
	}

	private void fixTrainedModel() {
		vocab.values().stream().forEach(node -> node.fixed = true);
		vocabChar.values().stream().forEach(node -> node.fixed = true);
		trainCharPreMap.values().stream().forEach(node -> node.fixed = true);
	}

	public CEmbedding infer(Iterable<Document> docs) {
		alpha = param.optm_initAlpha;
		try {
			Iterable<Document> fdocs = Iterables.filter(docs, doc -> !trainCharPreMap.containsKey(doc.id));

			HashMap<String, NodeWord> inferDocCharMap = new HashMap<>();
			fdocs.forEach(doc -> inferDocCharMap.put(doc.id, new NodeWord(doc.id, 1)));
			RandL rd = new RandL(1);
			inferDocCharMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

			long tknTotalInDocs = StreamSupport.stream(fdocs.spliterator(), false)
					.flatMap(doc -> doc.sentences.stream()).flatMap(sent -> Arrays.stream(sent.tokens))
					.filter(tkn -> vocab.containsKey(tkn)).count();

			gradientDecend(fdocs, inferDocCharMap, tknTotalInDocs, 0);

			CEmbedding embeddings = new CEmbedding();

			embeddings.charEmbedding = inferDocCharMap.entrySet()//
					.stream()//
					.map(ent -> new EntryPair<>(ent.getKey(), ent.getValue().neuIn))
					.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

			StreamSupport.stream(docs.spliterator(), false)//
					.map(doc -> trainCharPreMap.get(doc.id))//
					.filter(entry -> entry != null)//
					.forEach(entry -> {
						embeddings.charEmbedding.put(entry.token, entry.neuIn);
					});

			return embeddings;
		} catch (Exception e) {
			logger.info("Failed to learn new doc vector.", e);
			return null;
		}
	}

	public WordEmbedding produce() {
		WordEmbedding embedding = new WordEmbedding();
		embedding.vocabL = vocabL.stream().map(node -> new EntryPair<>(node.token, convertToFloat(node.neuIn)))
				.collect(toList());
		try {
			embedding.param = (new ObjectMapper()).writeValueAsString(this.param);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize the parameter. ", e);
		}
		return embedding;
	}

	public CEmbedding produceDocEmbd() {
		CEmbedding embedding = new CEmbedding();
		embedding.charEmbedding = trainCharPreMap.entrySet().stream().collect(
				toMap(ent -> ent.getKey(), ent -> Arrays.copyOf(ent.getValue().neuIn, ent.getValue().neuIn.length)));
		return embedding;
	}

	public LearnerChar2VecEmbedding(C2VParam param) {
		this.param = param;
	}

	public static class CEmbedding {
		public Map<String, double[]> charEmbedding;
	}

	public static ArrayList<String> charNgram(String token, int n) {
		ArrayList<String> result = new ArrayList<>();
		char[] characters = token.toCharArray();
		char[] gram = new char[n];
		for (int i = 0; i < characters.length - n + 1; ++i) {
			for (int j = 0; j < n; ++j)
				gram[j] = characters[i + j];
			result.add(String.valueOf(gram));
		}
		return result;
	}

	public static ArrayList<String> charNgramCf2(String token, int ns) {
		ArrayList<String> result = new ArrayList<>();
		IntStream.range(2, ns + 1).forEach(n -> {
			char[] characters = token.toCharArray();
			char[] gram = new char[n];
			for (int i = 0; i < characters.length - n + 1; ++i) {
				for (int j = 0; j < n; ++j)
					gram[j] = characters[i + j];
				result.add(String.valueOf(gram));
			}
		});
		return result;
	}
}
