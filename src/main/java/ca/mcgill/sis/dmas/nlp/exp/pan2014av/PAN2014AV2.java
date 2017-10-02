package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;

import auc.Confusion;
import auc.ReadList;
import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.collection.Counter;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.StreamIterable;
import ca.mcgill.sis.dmas.io.file.DmasFileOperations;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsOpennlp;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NlpUtilsStandford;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import ca.mcgill.sis.dmas.nlp.corpus.preprocess.Preprocessor;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;

public class PAN2014AV2 {

	public static enum DocType {
		reviews, essays, novels, articles
	}

	private static Logger logger = LoggerFactory.getLogger(PAN2014AV2.class);

	private static ArrayList<EntryPair<String, String>> loadTruth(String file) {
		try {
			Lines lines = Lines.fromFileFullyCached(file, Charsets.UTF_8);
			ArrayList<EntryPair<String, String>> truths = new ArrayList<>();
			lines.forEach(line -> {
				if (line.trim().length() > 0) {
					if (line.startsWith(StringResources.REGEX_UTF8_BOM)) {
						line = line.substring(1);
					}
					String[] tokens = line.split(StringResources.STR_TOKENBREAK);
					if (tokens.length != 2)
						System.out.println("Warning: invalid truth :" + line);
					truths.add(new EntryPair<String, String>(tokens[0], tokens[1]));
				}
			});
			return truths;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static LanguageDataset generateLangDatasetFromRaw(String inputFolder, boolean seperatePunctutation,
			Language lang) {

		Tagger tagger = Tagger.getTagger(lang);

		Preprocessor preprocessor;
		if (seperatePunctutation)
			preprocessor = new Preprocessor(Preprocessor.F_ToLowerCase(), Preprocessor.F_SeperatePunctuation(),
					Preprocessor.F_RemoveEtraSpace());
		else
			preprocessor = new Preprocessor(Preprocessor.F_ToLowerCase(), Preprocessor.F_RemoveEtraSpace());
		File root = new File(inputFolder);
		logger.info("Processing {}", inputFolder);
		Stream<Document> allDocStream = Arrays.stream(root.listFiles()).filter(caseFolder -> caseFolder.isDirectory())
				.flatMap(caseFolder -> {
					ArrayList<Document> list = new ArrayList<>();
					{
						Document knownDoc = new Document();
						knownDoc.id = caseFolder.getName() + "-known";
						ArrayList<String> knLines = new ArrayList<>();
						DmasFileOperations.select(caseFolder.getAbsolutePath(), "^known").forEach(kf -> {
							try {
								String line = Lines.readAll(kf.getAbsolutePath(), Charsets.UTF_8, false);
								if (line.startsWith(StringResources.REGEX_UTF8_BOM)) {
									line = line.substring(1);
								}
								knLines.add(preprocessor.pass(line));
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						knownDoc.rawContent = StringResources.JOINER_TOKEN.join(knLines);
						// if (lang.equals(Language.english))
						knownDoc.sentences.add(new Sentence(knownDoc.rawContent, Tokenizer.tokenizerStandford));
						// else
						// knownDoc.sentences.add(new
						// Sentence(knownDoc.rawContent,
						// Tokenizer.tokenizerDefault));
						knownDoc.sentences_tags = knownDoc.sentences.stream().map(sent -> {
							Sentence tSentence = new Sentence();
							List<String> tags = tagger.tag(sent.tokens);
							if (sent.tokens.length != tags.size())
								logger.error("Unmatched size {} vs {}", sent.tokens.length, tags.size());
							tSentence.tokens = tags.toArray(new String[tags.size()]);
							return tSentence;
						}).collect(Collectors.toCollection(ArrayList::new));
						list.add(knownDoc);
					}
					{
						Document unknownDoc = new Document();
						unknownDoc.id = caseFolder.getName() + "-unknown";
						ArrayList<String> unknLines = new ArrayList<>();
						DmasFileOperations.select(caseFolder.getAbsolutePath(), "^unknown").forEach(unkf -> {
							try {
								String line = Lines.readAll(unkf.getAbsolutePath(), Charsets.UTF_8, false);
								if (line.startsWith(StringResources.REGEX_UTF8_BOM)) {
									line = line.substring(1);
								}
								unknLines.add(preprocessor.pass(line));
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						unknownDoc.rawContent = StringResources.JOINER_TOKEN.join(unknLines);
						unknownDoc.sentences.add(new Sentence(unknownDoc.rawContent, Tokenizer.tokenizerDefault));
						unknownDoc.sentences_tags = unknownDoc.sentences.stream().map(sent -> {
							Sentence tSentence = new Sentence();
							List<String> tags = tagger.tag(sent.tokens);
							tSentence.tokens = tags.toArray(new String[tags.size()]);
							return tSentence;
						}).collect(Collectors.toCollection(ArrayList::new));
						list.add(unknownDoc);
					}
					return list.stream();
				}).filter(doc -> doc != null);

		try {

			ArrayList<EntryPair<String, String>> truth = loadTruth(root.getAbsolutePath() + "//truth.txt");
			LanguageDataset ds = new LanguageDataset(truth, new StreamIterable<>(allDocStream), lang);
			return ds;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static void stats(String inputFolder, String outputFolder) throws Exception {
		File root = new File(inputFolder);
		SentenceDetector detector = SentenceDetector.sentenceDetectorStandford;
		Tokenizer tokenizer = Tokenizer.tokenizerStandford;

		Arrays.stream(root.listFiles()).filter(folder -> folder.isDirectory()).forEach(folder -> {
			try {
				Counter sentences = Counter.zero();
				Counter tokens = Counter.zero();
				LineSequenceWriter writer = Lines
						.getLineWriter(outputFolder + "//length_stats-" + folder.getName() + ".txt", false);

				Arrays.stream(folder.listFiles()).filter(caseFolder -> caseFolder.isDirectory()).forEach(caseFolder -> {

					DmasFileOperations.select(caseFolder.getAbsolutePath(), "^known").forEach(kf -> {
						try {
							String line = Lines.readAll(kf.getAbsolutePath(), Charsets.UTF_8, false);
							String[] sents = detector.detectSentences(line);
							if (sents == null)
								return;
							sentences.inc(sents.length);
							Counter doc_tokens = Counter.zero();
							Arrays.stream(sents).forEach(sent -> {
								String[] tkns = tokenizer.tokenize(sent);
								if (tkns == null)
									return;
								tokens.inc(tkns.length);
								doc_tokens.inc(tkns.length);
							});
							writer.writeLineNoExcept(Integer.toString(doc_tokens.getVal()));
						} catch (Exception e) {
							e.printStackTrace();
						}
					});

					DmasFileOperations.select(caseFolder.getAbsolutePath(), "^unknown").forEach(unkf -> {
						try {
							String line = Lines.readAll(unkf.getAbsolutePath(), Charsets.UTF_8, false);
							String[] sents = detector.detectSentences(line);
							if (sents == null)
								return;
							sentences.inc(sents.length);
							Counter doc_tokens = Counter.zero();
							Arrays.stream(sents).forEach(sent -> {
								String[] tkns = tokenizer.tokenize(sent);
								if (tkns == null)
									return;
								tokens.inc(tkns.length);
								doc_tokens.inc(tkns.length);
							});
							writer.writeLineNoExcept(Integer.toString(doc_tokens.getVal()));
						} catch (Exception e) {
							e.printStackTrace();
						}
					});

				});

				System.out.println(folder.getName() + "  " + sentences.getVal());
				System.out.println(folder.getName() + "  " + tokens.getVal());
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private static HashMap<String, LanguageDataset> processRawDataset(String datasetFolder,
			boolean seperatePunctuation) {
		File root = new File(datasetFolder);
		if (!root.isDirectory()) {
			logger.error("Failed to load root folder of the raw dataset; the input {} is not a directory.",
					datasetFolder);
			return null;
		}

		HashMap<String, LanguageDataset> ds = new HashMap<>();
		Arrays.stream(root.listFiles()).filter(file -> file.isDirectory()).forEach(file -> {
			Language currentLang = null;
			for (Language lang : Language.values())
				if (file.getName().toLowerCase().contains(lang.toString().toLowerCase())) {
					currentLang = lang;
					break;
				}
			DocType currentType = null;
			for (DocType tp : DocType.values()) {
				if (file.getName().toLowerCase().contains(tp.toString().toLowerCase())) {
					currentType = tp;
					break;
				}
			}
			if (currentLang == null) {
				logger.error("Failed to detect language for dataset {}", file.getName());
				return;
			}
			LanguageDataset lds = generateLangDatasetFromRaw(file.getAbsolutePath(), seperatePunctuation, currentLang);
			ds.put(currentLang.toString() + "-" + currentType.toString(), lds);
		});

		return ds;
	}

	private static HashMap<String, LanguageDataset> loadProcessedDataset(String processedDatasetFolder) {
		File root = new File(processedDatasetFolder);
		if (!root.isDirectory()) {
			logger.error("Failed to load root folder of the raw dataset; the input {} is not a directory.",
					processedDatasetFolder);
			return null;
		}
		HashMap<String, LanguageDataset> ds = new HashMap<String, LanguageDataset>();
		Arrays.stream(root.listFiles()).filter(file -> file.isDirectory()).forEach(file -> {
			try {
				LanguageDataset ld = LanguageDataset.load(file.getAbsolutePath());
				ds.put(file.getName(), ld);
			} catch (IOException e) {
				logger.error("Failed to load dataset for " + file.getAbsolutePath(), e);
			}
		});
		return ds;
	}

	@JsonIgnore
	public HashMap<String, LanguageDataset> trainingSet;

	@JsonIgnore
	public HashMap<String, LanguageDataset> testingSet;

	private PAN2014AV2() {
	}

	public static PAN2014AV2 generateFromRaw(String rawTrainingDataSetFolder, String rawTestingDataSetFolder,
			boolean seperatePunctuation) {
		PAN2014AV2 ds = new PAN2014AV2();
		ds.trainingSet = processRawDataset(rawTrainingDataSetFolder, seperatePunctuation);
		ds.testingSet = processRawDataset(rawTestingDataSetFolder, seperatePunctuation);
		return ds;
	}

	/**
	 * folder structure: root -> train -> language; root -> test -> languages
	 * 
	 * @param outputFolder
	 */
	public void save(String outputFolder) {
		trainingSet.forEach((k, v) -> {
			v.save(outputFolder + "//train//" + k.toString());
		});
		testingSet.forEach((k, v) -> {
			v.save(outputFolder + "//test//" + k.toString());
		});
	}

	public static PAN2014AV2 load(String processedDatasetFolder) {

		PAN2014AV2 ds = new PAN2014AV2();
		ds.trainingSet = loadProcessedDataset(processedDatasetFolder + "//train//");
		ds.testingSet = loadProcessedDataset(processedDatasetFolder + "//test//");
		return ds;
	}

	private static double test(Verifier verifier, LanguageDataset dataset, boolean validation) {
		try {
			String tmpFile = File.createTempFile("stylometric-pan15-roc", ".txtF").getAbsolutePath();
			LineSequenceWriter writer = Lines.getLineWriter(tmpFile, false);
			ArrayList<EntryPair<String, String>> ls = new ArrayList<>(dataset.truthMapping);
			int limit = dataset.truthMapping.size();
			if (validation) {
				Collections.shuffle(ls, new Random());
				limit = dataset.truthMapping.size() / 2;
			}
			ls.stream().limit(limit).forEach(truth -> {
				String group = truth.key;
				String label = truth.value;

				List<String> keys = verifier.getKeys().stream().filter(key -> key.contains(group + "-known"))
						.collect(Collectors.toList());
				if (keys.size() < 1)
					logger.error("There should be more than known file per case. {}", group);

				List<String> unkeys = verifier.getKeys().stream().filter(key -> key.contains(group + "-unknown"))
						.collect(Collectors.toList());
				if (unkeys.size() != 1)
					logger.error("There should be only one unknown file per case. {}", group);
				double score = verifier.similarity(keys.get(0), unkeys.get(0));
				writer.writeLine(Double.toString(score), Integer.toString(label.equalsIgnoreCase("Y") ? 1 : 0));
			});
			writer.close();
			Confusion fusion = ReadList.readFile(tmpFile, "list");
			double roc = fusion.calculateAUCROC();
			return roc;
		} catch (Exception e) {
			logger.error("Failed to create line writer. ", e);
			return 0.5;
		}
	}

	public double validate(Verifier verifier, String lang) {
		return test(verifier, this.trainingSet.get(lang), true);
	}

	public double test(Verifier verifier, String lang) {
		return test(verifier, this.testingSet.get(lang), false);
	}

	public void printStat(HashMap<String, LanguageDataset> set) {
		HashMap<String, Integer> numberOfCases = new HashMap<>();
		HashMap<String, Integer> numberOfDocs = new HashMap<>();
		HashMap<String, Integer> numberOfTokens = new HashMap<>();
		HashMap<String, ArrayList<Integer>> tokensPerText = new HashMap<>();

		set.keySet().forEach(key -> {
			String id = key;
			LanguageDataset ds = set.get(key);
			numberOfCases.put(id, ds.truthMapping.size());
			numberOfDocs.put(id, Iterables.size(ds.documents));
			ArrayList<Integer> docTkns = new ArrayList<>();
			numberOfTokens.put(id, StreamSupport.stream(ds.documents.spliterator(), false).mapToInt(doc -> {
				int tkns = doc.sentences.stream().mapToInt(sent -> sent.tokens.length).sum();
				docTkns.add(tkns);
				return tkns;
			}).sum());
			tokensPerText.put(id, docTkns);
		});

		HashMap<String, List<Double>> numberOfTokensStat = new HashMap<>();
		tokensPerText.forEach((k, v) -> {
			DescriptiveStatistics stat = new DescriptiveStatistics();
			v.forEach(stat::addValue);
			List<Double> val = Arrays.asList(new Double[] { stat.getMean(), stat.getStandardDeviation() });
			numberOfTokensStat.put(k, val);
		});

		System.out.println(numberOfCases);
		System.out.println(numberOfDocs);
		System.out.println(numberOfTokens);
		System.out.println(numberOfTokensStat);
	}

	public static String WS_PATH = "D:/authorship-cby/pan2014-verification/";
	public static String DS_TRAIN_PATH = WS_PATH + "training/";
	public static String DS_TEST_PATH = WS_PATH + "testing/";
	public static String DS_PROCESSED_PATH = WS_PATH + "processed/";

	public static void main(String[] args) {
		String nlp_path = args[1];
		String dataset_path = args[0];
		String NLP_UTIL_PATH = nlp_path;
		NlpUtilsOpennlp.PATH_NLP_MODELS = NLP_UTIL_PATH + "\\opennlp\\";
		NlpUtilsStandford.PATH_STANDFORD_MODELS = NLP_UTIL_PATH + "\\standford\\";

		WS_PATH = dataset_path;
		DS_TRAIN_PATH = WS_PATH + "/training/";
		DS_TEST_PATH = WS_PATH + "/testing/";
		DS_PROCESSED_PATH = WS_PATH + "/processed/";

		PAN2014AV2 ds = generateFromRaw(DS_TRAIN_PATH, DS_TEST_PATH, true);
		ds.save(DS_PROCESSED_PATH);

		// PAN2014AV2
		ds = PAN2014AV2.load(DS_PROCESSED_PATH);
		// ds.printStat(ds.trainingSet);
		// ds.printStat(ds.testingSet);

	}
}
