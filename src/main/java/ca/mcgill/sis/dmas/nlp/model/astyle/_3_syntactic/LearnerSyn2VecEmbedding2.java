package ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.IteratorSafeGen;
import ca.mcgill.sis.dmas.io.collection.Pool;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.GradientProgress;
import ca.mcgill.sis.dmas.nlp.model.astyle.NodeWord;
import ca.mcgill.sis.dmas.nlp.model.astyle.RandL;
import ca.mcgill.sis.dmas.nlp.model.astyle.WordEmbedding;
import ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic.LearnerSyn2VecEmbedding.S2VParam;
import ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic.LearnerSyn2VecEmbedding.SEmbedding;

import static ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;

import static java.lang.Math.sqrt;

public class LearnerSyn2VecEmbedding2 {

	private static Logger logger = LoggerFactory.getLogger(LearnerSyn2VecEmbedding2.class);

	public Map<String, NodeWord> vocab = null;
	public Map<String, NodeWord> vocabS = null;
	public Map<String, NodeWord> trainSynPreMap = null;
	public List<NodeWord> vocabL = null;
	public int[] pTable;
	public transient volatile double alpha;
	public transient volatile long tknCurrent;
	public transient volatile long tknLastUpdate;
	public volatile long tknTotal;
	public volatile boolean debug = true;
	public S2VParam param;

	public Consumer<Integer> iterationHood = null;

	private void preprocess(Iterable<Document> documents) {

		vocab = null;

		// frequency map:
		final HashMap<String, Long> counter = new HashMap<>();
		documents.forEach(doc -> doc.sentences.forEach(sent -> sent
				.forEach(token -> counter.compute(token.trim().toLowerCase(), (w, c) -> c == null ? 1 : c + 1))));

		// add additional word (for /n)
		counter.put("</s>", Long.MAX_VALUE);

		// create word nodes (matrix)
		vocab = counter.entrySet().stream().parallel().filter(en -> en.getValue() >= param.min_freq)
				.collect(toMap(Map.Entry::getKey, p -> new NodeWord(p.getKey(), p.getValue())));

		// total valid word count
		tknTotal = vocab.values().stream().filter(w -> !w.token.equals("</s>")).mapToLong(w -> w.freq).sum();

		// initialize matrix:
		RandL rd = new RandL(1);
		vocab.values().stream().forEach(node -> node.initInLayer(param.vec_dim, rd));

		// sub-sampling probability
		if (param.optm_subsampling > 0) {
			double fcount = param.optm_subsampling * tknTotal;
			vocab.values().stream().parallel().forEach(w -> w.samProb = (sqrt(w.freq / fcount) + 1) * fcount / w.freq);
		}

		// POS gram table:
		vocabS = new HashMap<>();
		documents.forEach(doc -> doc.sentences_tags//
				.forEach(sent -> this.ngram(sent.tokens, 2)//
						.forEach(pos -> vocabS.compute(pos.toUpperCase(), (k, v) -> {
							if (v != null) {
								v.freq++;
								return v;
							} else {
								return new NodeWord(k, 1);
							}
						}))));
		vocabS.values().forEach(node -> node.initOutLayer(param.vec_dim));

		// vocabulary list (sorted)
		vocabL = vocabS.values().stream().sorted((a, b) -> b.token.compareTo(a.token))
				.sorted((a, b) -> Double.compare(b.freq, a.freq)).collect(toList());

		pTable = createPTbl(vocabL, (int) 1e8, 0.75);

		// train doc map:
		trainSynPreMap = new HashMap<>();
		documents.forEach(doc -> trainSynPreMap.put(doc.id, new NodeWord(doc.id, 1)));
		trainSynPreMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

		if (debug)
			logger.info("Vocab {}; Total char vocab:{}; Total words {};", vocabL.size(), vocabS.size(), tknTotal);

	}

	private void gradientDecend(final Iterable<Document> documents, Map<String, NodeWord> posPreMap, long numTkns,
			int alphaUpdateInterval) throws InterruptedException, ExecutionException {
		tknLastUpdate = 0;
		tknCurrent = 0;
		// training
		GradientProgress p = new GradientProgress(numTkns * param.optm_iteration);
		if (debug)
			p.start(logger);

		// thread-safe batch consumer generator:
		final IteratorSafeGen<Document> gen = new IteratorSafeGen<>(documents, 100, param.optm_iteration,
				iterationHood);

		new Pool(param.optm_parallelism).start(indx -> {
			RandL rl = new RandL(indx);
			double[] bfIn = new double[param.vec_dim], bfNeul1e = new double[param.vec_dim];
			gen.subIterable().forEach(doc -> {
				for (Sentence[] sent : doc.taggedSentences()) {

					// update alpha:
					if (param.optm_aphaUpdateInterval > 0
							&& tknCurrent - tknLastUpdate > param.optm_aphaUpdateInterval) {
						alpha = param.optm_initAlpha * (1.0 - 1.0 * tknCurrent / (numTkns * param.optm_iteration + 1));
						alpha = alpha < param.optm_initAlpha * 0.0001 ? param.optm_initAlpha * 0.0001 : alpha;
						if (debug)
							p.report(logger, tknCurrent, alpha);
						tknLastUpdate = tknCurrent;
					}

					// dictionary lookup & sub-sampling
					if (sent[0].tokens.length != sent[1].tokens.length)
						logger.warn("Find a bad tagged sentence. Size unmatched.");

					int[] inds = IntStream.range(0, sent[0].tokens.length).filter(ind -> {
						NodeWord node = vocab.get(sent[0].tokens[ind]);
						if (node == null)
							return false;
						if (node.samProb > rl.nextF())
							return true;
						else
							return false;
					}).peek(ind -> tknCurrent++).toArray();

					List<EntryPair<NodeWord, List<NodeWord>>> contexts = new ArrayList<>();
					Arrays.stream(inds).forEach(ind -> {
						NodeWord word = vocab.get(sent[0].tokens[ind]);
						List<NodeWord> context = new ArrayList<>();
						context.addAll(createContext(ind, sent[1].tokens, 0));
						// context.addAll(createContext(ind, sent[1].tokens,1));
						contexts.add(new EntryPair<NodeWord, List<NodeWord>>(word, context));
					});
					// System.out.println(contexts);

					if (!posPreMap.containsKey(doc.id)) {
						logger.error("Critical error. Doc node not found {}", doc);
						return;
					}

					iterate(contexts, posPreMap.get(doc.id), rl, bfIn, bfNeul1e);
				}
			});
		}).waiteForCompletion();
		if (debug)
			p.complete(logger);
	}

	private List<NodeWord> createContext(int ind, String[] tokens, int offset) {
		List<NodeWord> context = new ArrayList<>();
		int leftStart = ind - offset - 2;
		int leftEnd = ind - offset - 1;
		if (leftStart > 0 && leftEnd > 0) {
			String leftPos = tokens[leftStart].toUpperCase() + " " + tokens[leftEnd].toUpperCase();
			NodeWord left = vocabS.get(leftPos);
			if (left != null)
				context.add(left);
		}

		int rightStart = ind + offset + 1;
		int rightEnd = ind + offset + 2;
		if (rightStart < tokens.length && rightEnd < tokens.length) {
			String rightPos = tokens[rightStart].toUpperCase() + " " + tokens[rightEnd].toUpperCase();
			NodeWord right = vocabS.get(rightPos);
			if (right != null)
				context.add(right);
		}
		return context;
	}

	// negative sampling
	private void iterate(List<EntryPair<NodeWord, List<NodeWord>>> nsent, NodeWord docPosNode, RandL rl, double[] bfIn,
			double[] bfNeul1e) {

		Supplier<NodeWord> wordNgMethod = () -> vocabL.get(pTable[//
		(int) Long.remainderUnsigned(rl.nextR() >>> 16, pTable.length)//
		]);

		nsent.forEach(cont -> {

			NodeWord word = cont.key;

			cont.value.stream().filter(pos -> pos != null).forEach(pos -> {
				Arrays.fill(bfNeul1e, 0.0);
				Arrays.fill(bfIn, 0.0);
				add(bfIn, word.neuIn);
				add(bfIn, docPosNode.neuIn);
				div(bfIn, 2);
				// this.softMax(pos, bfIn, this.vocabS.values(), bfNeul1e);
				this.ngSamp(pos, bfIn, bfNeul1e, wordNgMethod);
				if (!word.fixed)
					add(word.neuIn, bfNeul1e);
				if (!docPosNode.fixed)
					add(docPosNode.neuIn, bfNeul1e);
			});
		});

	}

	private void softMax(NodeWord tar, double[] in, Collection<NodeWord> candidates, double[] neul1e) {
		List<EntryPair<NodeWord, Double>> forward = candidates//
				.stream()//
				.map(node -> {
					double val = Math.exp(dot(in, node.neuOut));// result can be
																// positive
																// infinity
					if (Double.isInfinite(val))
						val = 1;
					else if (Double.isNaN(val))
						val = 0;
					return new EntryPair<>(node, val);
				})//
				.collect(Collectors.toList());
		double sum = forward.stream().mapToDouble(ent -> ent.value).sum();
		if (Double.isNaN(sum) || Double.isInfinite(sum))
			System.out.println("Error.");
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
		sub(neul1e, tar.neuOut);
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
		gradientDecend(docs, trainSynPreMap, tknTotal, param.optm_aphaUpdateInterval);
		fixTrainedModel();
	}

	private void fixTrainedModel() {
		vocab.values().stream().forEach(node -> node.fixed = true);
		vocabS.values().stream().forEach(node -> node.fixed = true);
		trainSynPreMap.values().stream().forEach(node -> node.fixed = true);
	}

	public SEmbedding infer(Iterable<Document> docs) {
		alpha = param.optm_initAlpha;
		try {
			Iterable<Document> fdocs = Iterables.filter(docs, doc -> !trainSynPreMap.containsKey(doc.id));

			HashMap<String, NodeWord> inferDocCharMap = new HashMap<>();
			fdocs.forEach(doc -> inferDocCharMap.put(doc.id, new NodeWord(doc.id, 1)));
			RandL rd = new RandL(1);
			inferDocCharMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

			long tknTotalInDocs = StreamSupport.stream(fdocs.spliterator(), false)
					.flatMap(doc -> doc.sentences.stream())
					.flatMap(sent -> Arrays.asList(sent.tokens).subList(0, sent.tokens.length).stream())
					.filter(tkn -> vocab.containsKey(tkn)).count();

			gradientDecend(fdocs, inferDocCharMap, tknTotalInDocs, 0);

			SEmbedding embeddings = new SEmbedding();

			embeddings.synEmbedding = inferDocCharMap.entrySet()//
					.stream()//
					.map(ent -> new EntryPair<>(ent.getKey(), ent.getValue().neuIn))
					.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

			StreamSupport.stream(docs.spliterator(), false)//
					.map(doc -> trainSynPreMap.get(doc.id))//
					.filter(entry -> entry != null)//
					.forEach(entry -> {
						embeddings.synEmbedding.put(entry.token, entry.neuIn);
					});

			return embeddings;
		} catch (Exception e) {
			logger.info("Failed to learn new doc vector.", e);
			return null;
		}
	}

	public WordEmbedding produce() {
		WordEmbedding embedding = new WordEmbedding();
		embedding.vocabL = vocab.values().stream().map(node -> new EntryPair<>(node.token, convertToFloat(node.neuIn)))
				.collect(toList());
		try {
			embedding.param = (new ObjectMapper()).writeValueAsString(this.param);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize the parameter. ", e);
		}
		return embedding;
	}

	public SEmbedding produceDocEmbd() {
		SEmbedding embedding = new SEmbedding();
		embedding.synEmbedding = trainSynPreMap.entrySet().stream().collect(
				toMap(ent -> ent.getKey(), ent -> Arrays.copyOf(ent.getValue().neuIn, ent.getValue().neuIn.length)));
		return embedding;
	}

	public LearnerSyn2VecEmbedding2(S2VParam param) {
		this.param = param;
	}

	public ArrayList<String> ngram(String[] tokens, int n) {
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < tokens.length - n + 1; ++i) {
			result.add(tokens[i] + " " + tokens[i + 1]);
		}
		return result;
	}

}
