package ca.mcgill.sis.dmas.nlp.exp.pan2013ap;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.arff.ArffVectorLabelFile;
import ca.mcgill.sis.dmas.io.arff.LibLINEAR;
import ca.mcgill.sis.dmas.io.collection.StreamIterable;
import ca.mcgill.sis.dmas.io.file.DmasFileOperations;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsOpennlp;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsStandford;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import ca.mcgill.sis.dmas.nlp.corpus.preprocess.Preprocessor;
import ca.mcgill.sis.dmas.nlp.exp.pan2013ap.PAN13Utils.EvaluationResult;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * each paragrah represents a document (several blogs)
 * 
 * @author steven
 *
 */
public class PAN2013AP {

	private static Logger logger = LoggerFactory.getLogger(PAN2013AP.class);

	public static class P13LanguageDataset {

		@JsonIgnore
		public Iterable<Document> trainingDocs;
		@JsonIgnore
		public Iterable<Document> testDocs;

		public HashMap<String, String> trainingLabelAge = new HashMap<>();
		public HashMap<String, String> trainingLabelGender = new HashMap<>();
		public HashMap<String, String> testLabelAge = new HashMap<>();
		public HashMap<String, String> testLabelGender = new HashMap<>();

		public Language language;

		public void save(String directory) {
			File dir = new File(directory);
			if (!dir.exists())
				dir.mkdirs();
			Document.writeToFile(trainingDocs, new File(dir.getAbsolutePath() + "//trainingDoc.json"));
			Document.writeToFile(testDocs, new File(dir.getAbsolutePath() + "//testingDoc.json"));
			try {
				(new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValue(new File(directory + "//meta.json"),
						this);
			} catch (Exception e) {
				logger.error("Failed to save PAN2013 dataset to " + directory, e);
			}
		}

		public static P13LanguageDataset load(String directory) {
			try {
				P13LanguageDataset pan = (new ObjectMapper()).readValue(new File(directory + "//meta.json"),
						P13LanguageDataset.class);
				pan.trainingDocs = Document.loadFromFile(new File(directory + "//trainingDoc.json"));
				pan.testDocs = Document.loadFromFile(new File(directory + "//testingDoc.json"));
				return pan;
			} catch (Exception e) {
				logger.error("Failed to load dataset from " + directory, e);
			}
			return null;
		}
	}

	public HashMap<Language, P13LanguageDataset> datasets = new HashMap<>();

	public EvaluationResult test(Map<String, double[]> topical, Language lang) {
		logger.info("testing..");
		return test(topical, lang, false, false);
	}

	public EvaluationResult test(Map<String, double[]> topical, Language lang, boolean skipAge, boolean skipGender) {
		P13LanguageDataset dataset = datasets.get(lang);
		EvaluationResult result = new EvaluationResult();
		if (!skipAge) {
			double[] acc_age = test(topical, dataset.trainingLabelAge, dataset.testLabelAge);
			result.age_validation = acc_age[0];
			result.age_test = acc_age[1];
		}
		if (!skipGender) {
			double[] acc_gender = test(topical, dataset.trainingLabelGender, dataset.testLabelGender);
			result.gender_validation = acc_gender[0];
			result.gender_test = acc_gender[1];
		}
		return result;
	}

	private double[] test(Map<String, double[]> topical, HashMap<String, String> traningLabel,
			HashMap<String, String> testLabel) {

		File tmpFolder = new File(PAN2013AP.WK_PATH + "/tmp/");
		if (!tmpFolder.exists())
			tmpFolder.mkdir();

		double[] acc = new double[2];
		int dim = topical.values().stream().findAny().get().length;
		HashSet<String> labels = traningLabel.values().stream().collect(Collectors.toCollection(HashSet::new));
		try {
			ArffVectorLabelFile trainFile = new ArffVectorLabelFile(
					Files.createTempFile(tmpFolder.toPath(), "stylo-training", ".arff").toFile().getAbsolutePath(), dim,
					labels);
			traningLabel.keySet().forEach(docId -> {
				double[] vec = topical.get(docId);
				trainFile.push(vec, traningLabel.get(docId));
			});
			trainFile.close();
			Instances trainData = DataSource.read(trainFile.path);
			trainData.setClassIndex(trainData.numAttributes() - 1);

			ArffVectorLabelFile testFile = new ArffVectorLabelFile(
					Files.createTempFile(tmpFolder.toPath(), "stylo-testing", ".arff").toFile().getAbsolutePath(), dim,
					labels);
			testLabel.keySet().forEach(docId -> {
				double[] vec = topical.get(docId);
				testFile.push(vec, testLabel.get(docId));
			});
			testFile.close();
			Instances testData = DataSource.read(testFile.path);
			testData.setClassIndex(testData.numAttributes() - 1);

			Classifier classifier = new LibLINEAR();
			classifier.buildClassifier(trainData);
			
			logger.info("classifying...");

			{
				Evaluation eval = new Evaluation(trainData);
				for (Instance testInstance : testData) {
					double[] dist = classifier.distributionForInstance(testInstance);
					eval.evaluateModelOnce(dist, testInstance);
				}
				acc[1] = eval.weightedTruePositiveRate();
			}
			{
				Evaluation eval = new Evaluation(trainData);
				for (Instance validateInstance : trainData) {
					double[] dist = classifier.distributionForInstance(validateInstance);
					eval.evaluateModelOnce(dist, validateInstance);
				}
				acc[0] = eval.weightedTruePositiveRate();
			}
			return acc;
		} catch (Exception e) {
			logger.error("Failed to test the label set " + labels, e);
			return null;
		}
	}

	public void save(String path) {
		File folder = new File(path);
		if (!folder.exists())
			folder.mkdirs();
		datasets.forEach((k, v) -> {
			File subFolder = new File(folder.getAbsolutePath() + "//" + k + "//");
			if (!subFolder.exists())
				subFolder.mkdirs();
			v.save(subFolder.getAbsolutePath());
		});
	}

	public void stat() {
		datasets.values().forEach(ds -> {
			System.out.println(ds.language);
			System.out.println(ds.trainingLabelGender.size());
			System.out.println(ds.trainingLabelAge.size());
			System.out.println(ds.testLabelGender.size());
			System.out.println(ds.testLabelAge.size());
		});
	}

	public static PAN2013AP load(String path) {
		PAN2013AP pan2013ac = new PAN2013AP();
		File folder = new File(path);
		for (File subFolder : folder.listFiles()) {
			String key = subFolder.getName();
			P13LanguageDataset ds = P13LanguageDataset.load(subFolder.getAbsolutePath());
			pan2013ac.datasets.put(Language.valueOf(key), ds);
		}
		return pan2013ac;
	}

	public static PAN2013AP constructFromRaw(String trainingFolder, String testFolder) throws Exception {
		PAN2013AP pan2013ac = new PAN2013AP();
		P13LanguageDataset en = loadDataSetFromPANFormat(trainingFolder, testFolder, Language.english);
		P13LanguageDataset es = loadDataSetFromPANFormat(trainingFolder, testFolder, Language.spanish);
		pan2013ac.datasets.put(Language.english, en);
		pan2013ac.datasets.put(Language.spanish, es);
		return pan2013ac;
	}

	private static XPath xpath = XPathFactory.newInstance().newXPath();
	private static String expression = "/author/conversations/conversation";

	private static Preprocessor preprocessor = new Preprocessor(
			/* Preprocessor.F_RemoveNonASCII(), */
			Preprocessor.F_RemoveEtraSpace(),
			/* Preprocessor.F_ToLowerCase(), */
			Preprocessor.F_ReplaceToken(StringResources.REGEX_URL, "#URL"));

	private static P13LanguageDataset loadDataSetFromPANFormat(String trainingPath, String testPath, Language lang)
			throws Exception {

		String langId = "en";
		if (lang == Language.spanish) {
			langId = "es";
		}
		Tagger tagger = Tagger.getTagger(lang);

		HashMap<String, File> map = new HashMap<>();
		for (File file : (new File(testPath + "/" + langId)).listFiles()) {
			String id = file.getName().substring(0, file.getName().indexOf("_" + langId + "_"));
			boolean existed = map.put(id, file) != null;
			if (existed)
				logger.error("Duplicated ID: {}", id);
		}
		for (File file : (new File(trainingPath + "/" + langId)).listFiles()) {
			String id = file.getName().substring(0, file.getName().indexOf("_" + langId + "_"));
			boolean existed = map.put(id, file) != null;
			if (existed)
				logger.error("Duplicated ID: {}", id);
		}

		P13LanguageDataset dataset = new P13LanguageDataset();
		{
			ArrayList<String> trainingTruths = Lines.readAllAsArray(trainingPath + "truth-" + langId + ".txt",
					Charsets.UTF_8, false);
			dataset.trainingDocs = new StreamIterable<>(trainingTruths.stream().map(line -> {
				String[] parts = line.toLowerCase().split(":::");

				if (parts.length < 3) {
					logger.error("Error Line [{}], cannot be splited by ::: into more than three part ", line);
					return null;
				}
				if (parts.length != 3) {
					logger.error("Error Line [{}], more than 3 parts, will use the first 3 parts ", line);
					return null;
				}

				try {

					String id = parts[0].trim();
					String gender = parts[1].trim().toLowerCase();
					String age = parts[2].trim().toLowerCase();
					File file = map.get(id);
					if (file == null) {
						logger.error("Error Line [{}] file nonexisted.", line);
						return null;
					}

					InputSource inputSource = new InputSource(file.getAbsolutePath());
					NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
					StringBuilder sBuilder = new StringBuilder();
					for (int i = 0; i < nodes.getLength(); ++i) {
						Node node = nodes.item(i);
						String text = node.getTextContent();
						sBuilder.append(StringResources.STR_LINEBREAK).append(text);
					}
					Document document = buildDocument(sBuilder.toString(), tagger);
					document.id = id;
					dataset.trainingLabelAge.put(document.id, age);
					dataset.trainingLabelGender.put(document.id, gender);
					return document;
				} catch (Exception e) {
					logger.error("Failed to process line " + line, e);
					return null;
				}

			}).filter(doc -> doc != null));
		}

		{
			ArrayList<String> testTruths = Lines.readAllAsArray(testPath + "truth-" + langId + ".txt", Charsets.UTF_8,
					false);
			dataset.testDocs = new StreamIterable<>(testTruths.stream().map(line -> {
				String[] parts = line.toLowerCase().split(":::");

				if (parts.length < 3) {
					logger.error("Error Line [{}], cannot be splited by ::: into more than three part ", line);
					return null;
				}
				if (parts.length != 3) {
					logger.error("Error Line [{}], more than 3 parts, will use the first 3 parts ", line);
					return null;
				}

				try {

					String id = parts[0].trim();
					String gender = parts[1].trim().toLowerCase();
					String age = parts[2].trim().toLowerCase();
					File file = map.get(id);
					if (file == null) {
						logger.error("Error Line [{}] file nonexisted.", line);
						return null;
					}

					InputSource inputSource = new InputSource(file.getAbsolutePath());
					NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
					StringBuilder sBuilder = new StringBuilder();
					for (int i = 0; i < nodes.getLength(); ++i) {
						Node node = nodes.item(i);
						String text = node.getTextContent();
						sBuilder.append(StringResources.STR_LINEBREAK).append(text);
					}
					Document document = buildDocument(sBuilder.toString(), tagger);
					document.id = id;
					dataset.testLabelAge.put(document.id, age);
					dataset.testLabelGender.put(document.id, gender);
					return document;
				} catch (Exception e) {
					logger.error("Failed to process line " + line, e);
					return null;
				}

			}).filter(doc -> doc != null));

		}
		dataset.language = lang;
		return dataset;
	}

	private static Document buildDocument(String text, Tagger tagger) {
		Document document = new Document();
		document.rawContent = preprocessor.pass(text);
		document.sentences
				.addAll(Arrays.stream(SentenceDetector.sentenceDetectorStandford.detectSentences(document.rawContent))
						.map(str -> new Sentence(str, Tokenizer.newTokenizerDefault())).collect(Collectors.toList()));
		document.sentences_tags = document.sentences.stream().map(sent -> {
			Sentence tSentence = new Sentence();
			List<String> tags = tagger.tag(sent.tokens);
			if (sent.tokens.length != tags.size())
				logger.error("Unmatched size {} vs {}", sent.tokens.length, tags.size());
			tSentence.tokens = tags.toArray(new String[tags.size()]);
			return tSentence;
		}).collect(Collectors.toCollection(ArrayList::new));

		return document;
	}

	public static void fix(String inFolder, String outFolder) throws Exception {
		File outFolderFile = new File(outFolder);
		if (!outFolderFile.exists())
			outFolderFile.mkdirs();
		DmasFileOperations.select(inFolder, DmasFileOperations.REGEX_ALL).forEach(file -> {
			try {
				Lines lines = Lines.fromFile(file.getAbsolutePath());
				LineSequenceWriter writer = Lines.getLineWriter(outFolderFile.getAbsolutePath() + "//" + file.getName(),
						false);
				lines.forEach(line -> {
					if (line.trim().toLowerCase().startsWith("null")) {
						System.out.println(line);
					} else {
						writer.writeLineNoExcept(line);
					}
				});
				writer.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		});
	}

	public static String WK_PATH = "E:/authorship-cyb/pan2013-profiling/";
	public static String DS_TRAIN_PATH = WK_PATH + "pan13-author-profiling-training-corpus-2013-01-09/";
	public static String DS_TEST_PATH = WK_PATH + "pan13-author-profiling-test-corpus2-2013-04-29/";
	public static String DS_PROCESSED_PATH = WK_PATH + "/processedv2/";

	public static void main(String[] args) throws Exception {

		String NLP_UTIL_PATH = "E:/authorship-cyb/nlps/";
		NlpUtilsOpennlp.PATH_NLP_MODELS = NLP_UTIL_PATH + "/opennlp/";
		NlpUtilsStandford.PATH_STANDFORD_MODELS = NLP_UTIL_PATH + "/standford/";

		PAN2013AP ds;
		// ds = constructFromRaw(DS_TRAIN_PATH, DS_TEST_PATH);

		// ds.save(DS_PROCESSED_PATH);

		// PAN2013AC
		ds = load(DS_PROCESSED_PATH);
		ds.stat();

	}

}
