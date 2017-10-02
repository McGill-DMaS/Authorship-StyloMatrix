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
import ca.mcgill.sis.dmas.nlp.model.astyle._2_character.*;
import ca.mcgill.sis.dmas.nlp.model.astyle._2_character.LearnerChar2VecEmbedding.C2VParam;
import ca.mcgill.sis.dmas.nlp.model.astyle._2_character.LearnerChar2VecEmbedding.CEmbedding;;

public class PAN14TestChar2Vec {

	public static Logger logger = LoggerFactory.getLogger(PAN14TestChar2Vec.class);

	public static void main(String... args) throws InterruptedException, ExecutionException {
		PAN2014AV2.DS_PROCESSED_PATH = new File(args[0]).getAbsolutePath();
		System.out.println("CH model #1");
		MathUtilities.createExpTable();
		test();
	}

	public static TestEntry<C2VParam> test(PAN2014AV2 ds, C2VParam param, String testCase, String cacheFile,
			boolean test) {

		TestEntry<C2VParam> entry = TestEntries.retrieve(cacheFile, C2VParam.class, param);
		if (!test && entry != null)
			return entry;

		LearnerChar2VecEmbedding p2v = new LearnerChar2VecEmbedding(param);
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
			CEmbedding embd = p2v.produceDocEmbd();
			EmbeddingVerifier chr = new EmbeddingVerifier(MathUtilities.normalize(embd.charEmbedding));
			double ts = ds.test(chr, testCase);
			logger.info("{} character {}", testCase, ts);
			TestEntry<C2VParam> ent = new TestEntry<LearnerChar2VecEmbedding.C2VParam>(ts, param);
			if (!test)
				TestEntries.update(cacheFile, C2VParam.class, ent);
			return ent;
		} catch (Exception e) {
			logger.error("Failed to train the model..", e);
			return new TestEntry<LearnerChar2VecEmbedding.C2VParam>(-1, param);
		}
	}

	public static void test() throws InterruptedException, ExecutionException {

		String resultCache = StringResources.getRootPath() + "/cache/" + PAN14TestChar2Vec.class.getSimpleName() + "/";
		if (!new File(resultCache).isDirectory())
			new File(resultCache).mkdirs();
		PAN2014AV2 ds = PAN2014AV2.load(PAN2014AV2.DS_PROCESSED_PATH);

		Supplier<C2VParam> default_supplier = () -> {
			C2VParam param = new C2VParam();
			param.optm_parallelism = 1;
			param.optm_aphaUpdateInterval = -1;
			return param;
		};

		ForkJoinPool pool = new ForkJoinPool(30);

		pool.submit(() -> {
			ds.trainingSet.keySet().parallelStream().forEach(testcase -> {
				String resultCatchFile = resultCache + testcase + ".json";
				C2VParam last_max_param = TestEntries.currentMax(resultCatchFile, C2VParam.class);
				if (last_max_param != null) {
					logger.info("Testing {} with validation param", testcase);
					TestEntry<C2VParam> max = test(ds, last_max_param, testcase, resultCatchFile, true);
					logger.info(" {} score: {}", testcase, max.score);
					return;
				}
			});
		}).get();
	}

}
