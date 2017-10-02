package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import auc.Confusion;
import auc.ReadList;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic.LearnerSyn2VecEmbedding.S2VParam;

public class PAN14Utils {

	private static Logger logger = LoggerFactory.getLogger(PAN14Utils.class);

	public static class ValueWrapper {
		public double val;
	}

	public static class EvaluationResult {
		public double rocResult_test;
		public double rocResult_validate;
		public String tcase;
		public String name;
		public String param;

		public EvaluationResult(String name, String param, String tcase) {
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

	public static double AUROCEvaluation(String fname, Map<String, double[]> map,
			ArrayList<EntryPair<String, String>> truths) {
		try {
			LineSequenceWriter writer = Lines.getLineWriter(fname, false);
			truths.forEach(truth -> {
				String group = truth.key;
				String label = truth.value;

				List<String> keys = map.keySet().stream().filter(key -> key.contains(group + "-known"))
						.collect(Collectors.toList());
				if (keys.size() < 1)
					logger.error("There should be more than known file per case. {}", group);

				List<String> unkeys = map.keySet().stream().filter(key -> key.contains(group + "-unknown"))
						.collect(Collectors.toList());
				if (unkeys.size() > 1)
					logger.error("There should be only one unknown file per case. {}", group);
				double[] vi = map.get(keys.get(0));
				double[] vj = map.get(unkeys.get(0));
				double score = MathUtilities.dot(vi, vj);
				writer.writeLine(Double.toString(score), Integer.toString(label.equalsIgnoreCase("Y") ? 1 : 0));
			});
			writer.close();
			Confusion fusion = ReadList.readFile(fname, "list");
			return fusion.calculateAUCROC();
		} catch (Exception e) {
			logger.error("Failed to create line writer. ", e);
			return -1;
		}
	}
}
