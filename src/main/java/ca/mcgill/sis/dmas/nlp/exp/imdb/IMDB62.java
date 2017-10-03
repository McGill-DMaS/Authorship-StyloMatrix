package ca.mcgill.sis.dmas.nlp.exp.imdb;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.arff.ArffTester;
import ca.mcgill.sis.dmas.io.arff.ArffVectorLabelFile;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import ca.mcgill.sis.dmas.nlp.corpus.preprocess.Preprocessor;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NLPUtilsInitializer;
import weka.classifiers.evaluation.Evaluation;

public class IMDB62 {

	private static Logger logger = LoggerFactory.getLogger(IMDB62.class);

	public HashMap<String, String> truths = new HashMap<>();

	public ArrayList<Document> documents = new ArrayList<>();

	public void save(String fileToBeSaved) {
		try {
			(new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValue(new File(fileToBeSaved), this);
		} catch (Exception e) {
			logger.error("Failed to save the dataset.", e);
		}
	}

	public static IMDB62 loadFromProcessed(String processedFile) {
		try {
			return (new ObjectMapper()).readValue(new File(processedFile), IMDB62.class);
		} catch (Exception e) {
			logger.error("Failed to load the dataset.", e);
			return null;
		}
	}

	public double[] test(Map<String, double[]> embedding) {
		logger.info("testing");
		try {
			File tmpDir = new File(IMDB62.WK_PATH + "/tmp/");
			if (!tmpDir.exists())
				tmpDir.mkdir();
			int dim = embedding.values().stream().findAny().get().length;
			HashSet<String> labels = new HashSet<>(truths.values());
			ArffVectorLabelFile file = new ArffVectorLabelFile(Files
					.createTempFile(tmpDir.toPath(), "authorship-test-imdb62" + StringResources.timeString(), ".arff")
					.toFile().getAbsolutePath(), dim, labels);

			truths.keySet().forEach(key -> {
				double[] embd = embedding.get(key);
				String author = truths.get(key);
				file.push(embd, author);
			});

			file.close();
			ArffTester tester = new ArffTester();
			logger.info("Testing file " + file.path);
			Evaluation eval = tester.CV10(file.path, false);
			double acc = eval.correct() / (eval.correct() + eval.incorrect());
			return new double[] { acc, eval.weightedFMeasure() };
		} catch (Exception e) {
			logger.error("Failed to test.", e);
			return null;
		}

	}

	public Evaluation testEval(Map<String, double[]> embedding) {
		try {
			int dim = embedding.values().stream().findAny().get().length;
			HashSet<String> labels = new HashSet<>(truths.values());
			ArffVectorLabelFile file = new ArffVectorLabelFile(
					Files.createTempFile("authorship-test-imdb62" + StringResources.timeString(), ".arff").toFile()
							.getAbsolutePath(),
					dim, labels);

			truths.keySet().parallelStream().forEach(key -> {
				double[] embd = embedding.get(key);
				String author = truths.get(key);
				file.push(embd, author);
			});

			file.close();
			ArffTester tester = new ArffTester();
			Evaluation eval = tester.CV10(file.path, false);
			return eval;
		} catch (Exception e) {
			logger.error("Failed to test.", e);
			return null;
		}
	}

	public static IMDB62 loadFromRaw(String datasetFile) {

		try {
			IMDB62 ds = new IMDB62();
			Tagger tagger = Tagger.getTagger(Language.english);

			Preprocessor preprocessor = new Preprocessor(Preprocessor.F_ToLowerCase(),
					Preprocessor.F_SeperatePunctuation(), Preprocessor.F_RemoveEtraSpace());

			Lines lines = Lines.fromFile(datasetFile);
			StreamSupport.stream(lines.spliterator(), false).map(line -> {
				String[] parts = line.split("\\t");
				if (parts.length != 6) {
					logger.error("Failed to parse line []. It should contain 6 parts", line);
					return null;
				}
				String reviewId = parts[0].trim();
				String authorId = parts[1].trim();
				String content = parts[4] + StringResources.STR_TOKENBREAK + parts[5];
				Document knownDoc = new Document();
				knownDoc.id = reviewId;
				knownDoc.rawContent = content;
				knownDoc.sentences
						.add(new Sentence(preprocessor.pass(knownDoc.rawContent), Tokenizer.tokenizerStandford));
				knownDoc.sentences_tags = knownDoc.sentences.stream().map(sent -> {
					Sentence tSentence = new Sentence();
					List<String> tags = tagger.tag(sent.tokens);
					if (sent.tokens.length != tags.size())
						logger.error("Unmatched size {} vs {}", sent.tokens.length, tags.size());
					tSentence.tokens = tags.toArray(new String[tags.size()]);
					return tSentence;
				}).collect(Collectors.toCollection(ArrayList::new));

				ds.truths.put(reviewId, authorId);

				return knownDoc;

			}).filter(doc -> doc != null).forEach(ds.documents::add);

			return ds;
		} catch (Exception e) {
			logger.error("Failed to load dataset.", e);
			return null;
		}
	}

	public void test(List<Map<String, double[]>> embds) {
		embds.stream().forEach(embd -> {
			Evaluation eval = testEval(embd);
			logger.info("Macro: " + eval.unweightedMacroFmeasure());
			logger.info("Micro: " + eval.unweightedMicroFmeasure());
			logger.info("Weighted: " + eval.weightedFMeasure());
			logger.info("Acc: " + eval.pctCorrect());
			logger.info("Acc(cal): " + (eval.correct() / (eval.correct() + eval.incorrect())));
		});
	}

	public void stat(String file) throws Exception {
		LineSequenceWriter writter = Lines.getLineWriter(file, false);

		documents.stream().mapToLong(doc -> doc.sentences.stream().flatMap(sent -> Arrays.stream(sent.tokens)).count())
				.forEach(val -> writter.writeLineNoExcept(Long.toString(val)));

		writter.close();
	}

	public static String WK_PATH = "E:/authorship-cyb/imdb-attribution/";
	public static String DS_PROCESSED_PATH = WK_PATH + "/processed.json";

	public static void main(String[] args) throws Exception {
		String NLP_UTIL_PATH = "E:/authorship-cyb/nlps/";
		NLPUtilsInitializer.initialize(NLP_UTIL_PATH);

		IMDB62 ds = loadFromRaw(WK_PATH + "/imdb62.txt");
		ds.save(DS_PROCESSED_PATH);

		ds = loadFromProcessed(DS_PROCESSED_PATH);
		ds.stat(WK_PATH + "/processed.stat.txt");

	}

}
