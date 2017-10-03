package ca.mcgill.sis.dmas.nlp.exp.pan2013ap;

import java.io.File;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.nlp.model.astyle._1_lexical.LearnerTL2VecEmbedding.TL2VParam;

public class PAN13Utils {

	private static Logger logger = LoggerFactory.getLogger(PAN13Utils.class);

	public static class EvaluationResult {

		public double age_validation;
		public double age_test;
		public double gender_validation;
		public double gender_test;

		public double averageTest() {
			return (age_test + gender_test) * 1.0 / 2;
		}

		public double averageValidation() {
			return (age_validation + gender_validation) * 1.0 / 2;
		}

		@Override
		public String toString() {
			try {
				return (new ObjectMapper()).writeValueAsString(this);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return Integer.toString(this.hashCode());
			}
		}
	}

	public static class EvaluationResultWrapper {
		public double rocResult_test;
		public double rocResult_validate;
		public String tcase;
		public String name;
		public String param;
		public EvaluationResult innerResult;

		public EvaluationResultWrapper(String name, String param, String tcase) {
			this.name = name;
			this.param = param;
			this.tcase = tcase;
		}

		@Override
		public String toString() {
			try {
				return (new ObjectMapper()).writeValueAsString(this);
			} catch (JsonProcessingException e) {
				logger.error("Failed to convert a result to string", e);
				return "";
			}
		}

		public void write(String path) {
			try {
				File file = new File(path);
				file.getParentFile().mkdirs();
				(new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValue(file, this);
			} catch (Exception e) {
				logger.error("Failed to save result.", e);
			}
		}
	}

	public static <T, K> void tune(T[] vals, Supplier<K> supplier, BiConsumer<T, K> setFieldInParam,
			Consumer<K> tester) {
		ForkJoinPool forkJoinPool = new ForkJoinPool(1);

		try {
			forkJoinPool.submit(() -> {
				IntStream.range(0, vals.length).parallel().forEach(i -> {
					logger.info("Running for {}", i);
					K param = supplier.get();
					setFieldInParam.accept(vals[i], param);
					tester.accept(param);
				});
			}).get();
		} catch (Exception e) {
			logger.error("Error in execution.", e);
		}
	}

}
