package ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.netlib.util.booleanW;
import org.netlib.util.doubleW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import ca.mcgill.sis.dmas.io.arff.ArffTester;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.IteratorSafeGen;
import ca.mcgill.sis.dmas.io.collection.Pool;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.GradientProgress;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import ca.mcgill.sis.dmas.nlp.model.astyle.NodeWord;
import ca.mcgill.sis.dmas.nlp.model.astyle.RandL;
import ca.mcgill.sis.dmas.nlp.model.astyle.WordEmbedding;
import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.LearnerTL2VecEmbedding.TL2VParam;
import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.LearnerTL2VecEmbedding.TLEmbedding;
import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

import static ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import static java.lang.Math.sqrt;

public class LearnerTL2VecEmbedding3 {

	private static Logger logger = LoggerFactory.getLogger(LearnerTL2VecEmbedding3.class);

	public Map<String, NodeWord> vocab = null;
	public Map<String, NodeWord> trainDocLexicMap = null;
	public Map<String, NodeWord> trainDocTopicMap = null;
	public List<NodeWord> vocabL = null;
	public int[] pTable;
	public transient volatile double alpha;
	public transient volatile long tknCurrent;
	public transient volatile long tknLastUpdate;
	public volatile long tknTotal;
	public volatile boolean debug = true;
	public volatile boolean earlystop = false;
	public TL2VParam param;

	public transient Consumer<Integer> iterationHood = null;

	volatile public int batchSize = 100;

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
		vocabL.stream().forEach(node -> node.init(param.vec_dim, rd));

		// sub-sampling probability
		if (param.optm_subsampling > 0) {
			double fcount = param.optm_subsampling * tknTotal;
			vocabL.stream().parallel().forEach(w -> w.samProb = (sqrt(w.freq / fcount) + 1) * fcount / w.freq);
		}

		pTable = createPTbl(vocabL, (int) 1e8, 0.75);

		if (debug)
			logger.info("Vocab {}; Total {};", vocabL.size(), tknTotal);

		trainDocLexicMap = new HashMap<>();
		trainDocTopicMap = new HashMap<>();
		documents.forEach(doc -> trainDocLexicMap.put(doc.id, new NodeWord(doc.id, 1)));
		documents.forEach(doc -> trainDocTopicMap.put(doc.id, new NodeWord(doc.id, 1)));
		trainDocLexicMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));
		trainDocTopicMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

	}

	private void gradientDecend(final Iterable<Document> documents, Map<String, NodeWord> docLexicMap,
			Map<String, NodeWord> docTopicMap, long numTkns, long alphaUpdateInterval)
			throws InterruptedException, ExecutionException {
		tknLastUpdate = 0;
		tknCurrent = 0;
		// training
		GradientProgress p = new GradientProgress(numTkns * param.optm_iteration);
		if (debug)
			p.start(logger);

		// new list of topic nodes for ng-sampling:
		ArrayList<NodeWord> docTL = new ArrayList<>(trainDocTopicMap.values());

		// thread-safe batch consumer generator:
		final IteratorSafeGen<Document> gen = new IteratorSafeGen<>(documents, this.batchSize, param.optm_iteration,
				this.iterationHood);

		new Pool(param.optm_parallelism).start(indx -> {
			RandL rl = new RandL(indx);
			double[] bfIn = new double[param.vec_dim], bfNeul1e = new double[param.vec_dim],
					bfNeul2e = new double[param.vec_dim];
			// gen.subIterable().forEach(doc -> {
			for (Document doc : gen.subIterable()) {
				for (Sentence sent : doc) {
					if (earlystop) {
						logger.info("Got early-stopping signal. Stopping current thread.");
						return;
					}
					// update alpha:
					if (alphaUpdateInterval > 0 && tknCurrent - tknLastUpdate > alphaUpdateInterval) {
						alpha = param.optm_initAlpha * (1.0 - 1.0 * tknCurrent / (numTkns * param.optm_iteration + 1));
						alpha = alpha < param.optm_initAlpha * 0.0001 ? param.optm_initAlpha * 0.0001 : alpha;
						if (debug)
							p.report(logger, tknCurrent, alpha);
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

					if (!docLexicMap.containsKey(doc.id)) {
						logger.error("Critical error. Doc node not found {}", doc);
						return;
					}

					iterate(docTL, nsent, docLexicMap.get(doc.id), docTopicMap.get(doc.id), rl, bfIn, bfNeul1e,
							bfNeul2e);
				}
			} // );
		}).waiteForCompletion();
		if (debug)
			p.complete(logger);

	}

	// negative sampling
	private void iterate(List<NodeWord> docTL, List<NodeWord> nsent, NodeWord docLexicNode, NodeWord docTopicNode,
			RandL rl, double[] bfIn, double[] bfNeul1e, double[] bfNeul2e) {

		Supplier<NodeWord> wordNgMethod = () -> vocabL.get(pTable[//
		(int) Long.remainderUnsigned(rl.nextR() >>> 16, pTable.length)//
		]);

		// Supplier<NodeWord> docTNgMethod = () -> docTL.get(//
		// (int) Long.remainderUnsigned(rl.nextR() >>> 16, docTL.size())//
		// );

		// topical:
		// nsent.forEach(tar -> {
		// Arrays.fill(bfNeul1e, 0.0);
		// ngSamp(tar, docTopicNode.neuIn, bfNeul1e, wordNgMethod);
		// if (!docTopicNode.fixed)
		// add(docTopicNode.neuIn, bfNeul1e);
		// });

		slidingWnd(nsent, param.optm_window, rl).forEach(cont -> {

			ArrayList<NodeWord> all = new ArrayList<>(cont.value);
			all.add(cont.key);
			all.forEach(tar -> {
				Arrays.fill(bfNeul1e, 0.0);
				ngSamp(tar, docTopicNode.neuIn, bfNeul1e, wordNgMethod);
				if (!docTopicNode.fixed)
					add(docTopicNode.neuIn, bfNeul1e);
			});

			// lexical:
			Arrays.fill(bfIn, 0.0);
			Arrays.fill(bfNeul2e, 0.0);
			cont.value.stream().forEach(src -> add(bfIn, src.neuIn));
			add(bfIn, docLexicNode.neuIn);
			add(bfIn, docTopicNode.neuIn);
			div(bfIn, cont.value.size() + 2);
			ngSamp(cont.key, bfIn, bfNeul2e, wordNgMethod);

			if (!docLexicNode.fixed)
				add(docLexicNode.neuIn, bfNeul2e);

			cont.value.stream().filter(src -> !src.fixed).forEach(src -> add(src.neuIn, bfNeul2e));

		});

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
		gradientDecend(docs, trainDocLexicMap, trainDocTopicMap, tknTotal, param.optm_aphaUpdateInterval);
		fixTrainedModel();
	}

	private void fixTrainedModel() {
		vocab.values().stream().forEach(node -> node.fixed = true);
		trainDocLexicMap.values().stream().forEach(node -> node.fixed = true);
		trainDocTopicMap.values().stream().forEach(node -> node.fixed = true);
	}

	public TLEmbedding inferUnnormalized(Iterable<Document> docs) {
		alpha = param.optm_initAlpha;
		try {
			Iterable<Document> fdocs = Iterables.filter(docs, doc -> !trainDocLexicMap.containsKey(doc.id));

			HashMap<String, NodeWord> inferDocLexicMap = new HashMap<>();
			HashMap<String, NodeWord> inferDocTopicMap = new HashMap<>();
			fdocs.forEach(doc -> inferDocLexicMap.put(doc.id, new NodeWord(doc.id, 1)));
			fdocs.forEach(doc -> inferDocTopicMap.put(doc.id, new NodeWord(doc.id, 1)));
			RandL rd = new RandL(1);
			inferDocLexicMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));
			inferDocTopicMap.values().forEach(node -> node.initInLayer(this.param.vec_dim, rd));

			long tknTotalInDocs = StreamSupport.stream(fdocs.spliterator(), false)
					.flatMap(doc -> doc.sentences.stream()).flatMap(sent -> Arrays.stream(sent.tokens))
					.filter(tkn -> vocab.containsKey(tkn)).count();

			gradientDecend(fdocs, inferDocLexicMap, inferDocTopicMap, tknTotalInDocs, 0);

			TLEmbedding embeddings = new TLEmbedding();

			embeddings.lexicEmbedding = inferDocLexicMap.entrySet()//
					.stream()//
					.map(ent -> new EntryPair<>(ent.getKey(), ent.getValue().neuIn))
					.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

			embeddings.topicEmbedding = inferDocTopicMap.entrySet()//
					.stream()//
					.map(ent -> new EntryPair<>(ent.getKey(), ent.getValue().neuIn))
					.collect(Collectors.toMap(ent -> ent.key, ent -> ent.value));

			StreamSupport.stream(docs.spliterator(), false)//
					.map(doc -> new EntryPair<>(trainDocLexicMap.get(doc.id), trainDocTopicMap.get(doc.id)))//
					.filter(entry -> entry.key != null)//
					.forEach(entry -> {
						embeddings.lexicEmbedding.put(entry.key.token, entry.key.neuIn);
						embeddings.topicEmbedding.put(entry.value.token, entry.key.neuIn);
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

	public TLEmbedding produceDocEmbdUnnormalized() {
		TLEmbedding embedding = new TLEmbedding();
		embedding.lexicEmbedding = trainDocLexicMap.entrySet().stream()
				.collect(Collectors.<Map.Entry<String, NodeWord>, String, double[]>toMap(ent -> ent.getKey(),
						ent -> Arrays.copyOf(ent.getValue().neuIn, ent.getValue().neuIn.length)));
		embedding.topicEmbedding = trainDocTopicMap.entrySet().stream()
				.collect(Collectors.<Map.Entry<String, NodeWord>, String, double[]>toMap(ent -> ent.getKey(),
						ent -> Arrays.copyOf(ent.getValue().neuIn, ent.getValue().neuIn.length)));
		return embedding;
	}

	public LearnerTL2VecEmbedding3(TL2VParam param) {
		this.param = param;
	}

	public LearnerTL2VecEmbedding3() {
	}

	public static void saveWekaModel(String directory, Classifier cls_tplx_politic, Classifier cls_tp_politic,
			Classifier cls_lx_politic, Classifier cls_tplx_age, Classifier cls_tp_age, Classifier cls_lx_age,
			Classifier cls_tplx_gender, Classifier cls_tp_gender, Classifier cls_lx_gender) throws Exception {

		if (!new File(directory).isDirectory())
			new File(directory).mkdirs();

		SerializationHelper.write(new File(directory + "/politic.tplx.cls.model").getAbsolutePath(), cls_tplx_politic);
		SerializationHelper.write(new File(directory + "/politic.tp.cls.model").getAbsolutePath(), cls_tp_politic);
		SerializationHelper.write(new File(directory + "/politic.lx.cls.model").getAbsolutePath(), cls_lx_politic);

		SerializationHelper.write(new File(directory + "/age.tplx.cls.model").getAbsolutePath(), cls_tplx_age);
		SerializationHelper.write(new File(directory + "/age.tp.cls.model").getAbsolutePath(), cls_tp_age);
		SerializationHelper.write(new File(directory + "/age.lx.cls.model").getAbsolutePath(), cls_lx_age);

		SerializationHelper.write(new File(directory + "/gender.tplx.cls.model").getAbsolutePath(), cls_tplx_gender);
		SerializationHelper.write(new File(directory + "/gender.tp.cls.model").getAbsolutePath(), cls_tp_gender);
		SerializationHelper.write(new File(directory + "/gender.lx.cls.model").getAbsolutePath(), cls_lx_gender);
	}

	public static Function<Document, HashMap<String, double[]>> loadWithWekaModel(String directory) throws Exception {
		LearnerTL2VecEmbedding3 repModel = LearnerTL2VecEmbedding3.load(directory);

		Classifier cls_tplx_politic = (Classifier) SerializationHelper
				.read(new File(directory + "/politic.tplx.cls.model").getAbsolutePath());
		Classifier cls_tp_politic = (Classifier) SerializationHelper
				.read(new File(directory + "/politic.tp.cls.model").getAbsolutePath());
		Classifier cls_lx_politic = (Classifier) SerializationHelper
				.read(new File(directory + "/politic.lx.cls.model").getAbsolutePath());

		Classifier cls_tplx_age = (Classifier) SerializationHelper
				.read(new File(directory + "/age.tplx.cls.model").getAbsolutePath());
		Classifier cls_tp_age = (Classifier) SerializationHelper
				.read(new File(directory + "/age.tp.cls.model").getAbsolutePath());
		Classifier cls_lx_age = (Classifier) SerializationHelper
				.read(new File(directory + "/age.lx.cls.model").getAbsolutePath());

		Classifier cls_tplx_gender = (Classifier) SerializationHelper
				.read(new File(directory + "/gender.tplx.cls.model").getAbsolutePath());
		Classifier cls_tp_gender = (Classifier) SerializationHelper
				.read(new File(directory + "/gender.tp.cls.model").getAbsolutePath());
		Classifier cls_lx_gender = (Classifier) SerializationHelper
				.read(new File(directory + "/gender.lx.cls.model").getAbsolutePath());

		Function<Document, HashMap<String, double[]>> test_func = doc -> {
			TLEmbedding rep = repModel.inferUnnormalized(Arrays.asList(doc));
			double[] rep_tplx = MathUtilities.merge(true, rep.lexicEmbedding, rep.topicEmbedding).get(doc.id);
			double[] rep_lx = MathUtilities.normalize(rep.lexicEmbedding).get(doc.id);
			double[] rep_tp = MathUtilities.normalize(rep.topicEmbedding).get(doc.id);
			HashMap<String, double[]> dist_map = new HashMap<>();

			try {

				dist_map.put("politic-tplx", ArffTester.Test_Single(cls_tplx_politic, rep_tplx));
				dist_map.put("politic-tp", ArffTester.Test_Single(cls_tp_politic, rep_tp));
				dist_map.put("politic-lx", ArffTester.Test_Single(cls_lx_politic, rep_lx));

				dist_map.put("age-tplx", ArffTester.Test_Single(cls_tplx_age, rep_tplx));
				dist_map.put("age-tp", ArffTester.Test_Single(cls_tp_age, rep_tp));
				dist_map.put("age-lx", ArffTester.Test_Single(cls_lx_age, rep_lx));

				dist_map.put("gender-tplx", ArffTester.Test_Single(cls_tplx_gender, rep_tplx));
				dist_map.put("gender-tp", ArffTester.Test_Single(cls_tp_gender, rep_tp));
				dist_map.put("gender-lx", ArffTester.Test_Single(cls_lx_gender, rep_lx));

			} catch (Exception e) {
				logger.error("Failed to test a single instance. ", e);
			}
			return dist_map;
		};

		return test_func;
	}

	public void save(String path) throws Exception {
		new ObjectMapper().writeValue(new File(path + "/rep.tplx.model"), this);
	}

	public static LearnerTL2VecEmbedding3 load(String path) throws Exception {
		return new ObjectMapper().readValue(new File(path + "/rep.tplx.model"), LearnerTL2VecEmbedding3.class);
	}

}
