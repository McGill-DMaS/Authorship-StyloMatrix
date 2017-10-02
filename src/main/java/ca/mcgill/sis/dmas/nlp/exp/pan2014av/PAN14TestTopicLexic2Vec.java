package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.nlp.exp.Utils.TestEntries;
import ca.mcgill.sis.dmas.nlp.exp.Utils.TestEntry;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.*;
import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.LearnerTL2VecEmbedding.TL2VParam;
import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.LearnerTL2VecEmbedding.TLEmbedding;

public class PAN14TestTopicLexic2Vec {

	public static Logger logger = LoggerFactory.getLogger(PAN14TestTopicLexic2Vec.class);

	public static void main(String... args) throws InterruptedException, ExecutionException {
		PAN2014AV2.DS_PROCESSED_PATH = new File(args[0]).getAbsolutePath();
		System.out.println("TL model #3 ");
		MathUtilities.createExpTable();
		test();
	}

	public static TestEntry<TL2VParam> test(PAN2014AV2 ds, TL2VParam param, String testCase, String cacheFile,
			boolean test) {

		TestEntry<TL2VParam> entry = TestEntries.retrieve(cacheFile, TL2VParam.class, param);
		if (!test && entry != null)
			return entry;

		LearnerTL2VecEmbedding3 p2v = new LearnerTL2VecEmbedding3(param);
		p2v.debug = false;
		p2v.iterationHood = iteration -> {
			// this is a blocking hood.
		};
		// learn representation
		LanguageDataset trainingSet = ds.trainingSet.get(testCase);
		LanguageDataset testingSet = ds.testingSet.get(testCase);
		ArrayList<Document> documents = new ArrayList<>();
		trainingSet.documents.forEach(documents::add);
		testingSet.documents.forEach(documents::add);
		Collections.shuffle(documents, new Random(0));
		// test performance on testing set
		try {
			p2v.train(documents);
			TLEmbedding embd = p2v.produceDocEmbdUnnormalized();
			EmbeddingVerifier merged = new EmbeddingVerifier(
					MathUtilities.merge(true, embd.topicEmbedding, embd.lexicEmbedding));
			EmbeddingVerifier tp = new EmbeddingVerifier(MathUtilities.normalize(embd.topicEmbedding));
			EmbeddingVerifier lx = new EmbeddingVerifier(MathUtilities.normalize(embd.lexicEmbedding));
			double ts = ds.test(merged, testCase);
			double ts_tp = ds.test(tp, testCase);
			double ts_lx = ds.test(lx, testCase);
			logger.info("{} merged {} topical {} lexical {}", testCase, ts, ts_tp, ts_lx);
			TestEntry<TL2VParam> ent = new TestEntry<LearnerTL2VecEmbedding.TL2VParam>(ts, param);
			if (!test)
				TestEntries.update(cacheFile, TL2VParam.class, ent);
			return ent;
		} catch (Exception e) {
			logger.error("Failed to train the model..", e);
			return new TestEntry<LearnerTL2VecEmbedding.TL2VParam>(-1, param);
		}
	}

	public static void test() throws InterruptedException, ExecutionException {

		String resultCache = StringResources.getRootPath() + "/cache/" + PAN14TestTopicLexic2Vec.class.getSimpleName() + "/";
		if (!new File(resultCache).isDirectory())
			new File(resultCache).mkdirs();
		PAN2014AV2 ds = PAN2014AV2.load(PAN2014AV2.DS_PROCESSED_PATH);

		Supplier<TL2VParam> default_supplier = () -> {
			TL2VParam param = new TL2VParam();
			param.optm_parallelism = 1;
			param.optm_aphaUpdateInterval = -1;
			return param;
		};

		ForkJoinPool pool = new ForkJoinPool(30);

		pool.submit(() -> {
			ds.trainingSet.keySet().parallelStream().forEach(testcase -> {
				String resultCatchFile = resultCache + testcase + ".json";
				TL2VParam last_max_param = TestEntries.currentMax(resultCatchFile, TL2VParam.class);
				if (last_max_param != null) {
					logger.info("Testing {} with validation param", testcase);
					TestEntry<TL2VParam> max = test(ds, last_max_param, testcase, resultCatchFile, true);
					logger.info(" {} score: {}", testcase, max.score);
					return;
				}
			});
		}).get();
	}

}
