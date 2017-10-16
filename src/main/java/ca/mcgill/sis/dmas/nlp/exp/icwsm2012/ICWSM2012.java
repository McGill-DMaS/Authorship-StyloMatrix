package ca.mcgill.sis.dmas.nlp.exp.icwsm2012;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.collection.Counter;
import ca.mcgill.sis.dmas.io.collection.StreamIterable;
import ca.mcgill.sis.dmas.io.file.DmasFileOperations;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.corpus.io.twitter.crawler.TweetCorpusDownloader;
import ca.mcgill.sis.dmas.nlp.corpus.parser.NLPUtilsInitializer;
import ca.mcgill.sis.dmas.nlp.corpus.preprocess.Preprocessor;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities;
import cmu.arktweetnlp.Tagger;
import cmu.arktweetnlp.Tagger.TaggedToken;

public class ICWSM2012 {

	private static Logger logger = LoggerFactory.getLogger(ICWSM2012.class);

	public static String WS_DIR = "E:/authorship-cyb/icwsm2012-profiling/";

	public static class TwitterUser {
		public long uid;
		public HashSet<Long> tids;
		public ArrayList<String> labels;
	}

	public static ArrayList<Long> extractIds(String fileName) {
		try {
			Lines user_lines = Lines.fromFile(fileName);
			ArrayListMultimap<Long, String> user_label = ArrayListMultimap.create();
			for (String line : user_lines) {
				String[] parts = line.split("\\s+");
				Long uid = Long.parseLong(parts[0]);
				for (int i = 1; i < parts.length; ++i)
					user_label.put(uid, parts[i].trim());
			}

			Lines t_lines = Lines.fromFile(fileName.replace(".users", ".tweets"));
			HashMultimap<Long, Long> user_twids = HashMultimap.create();
			for (String line : t_lines) {
				String[] ids = line.split("\\s+");
				Long uid = Long.parseLong(ids[0]);
				if (user_label.containsKey(uid))
					for (int i = 1; i < ids.length; ++i)
						user_twids.put(uid, Long.parseLong(ids[i]));
			}

			ArrayList<TwitterUser> users = user_label.keySet().stream().map(uid -> {
				TwitterUser user = new TwitterUser();
				user.uid = uid;
				user.labels = new ArrayList<>(user_label.get(uid));
				user.tids = new HashSet<>(user_twids.get(uid));
				return user;
			}).collect(Collectors.toCollection(ArrayList::new));

			ArrayList<Long> tids = new ArrayList<>(user_twids.values());

			ObjectMapper mapper = new ObjectMapper();
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName + ".users.labeled.txt"), users);

			LineSequenceWriter writer = Lines.getLineWriter(fileName + ".users.tids.txt", false);
			tids.forEach(tid -> writer.writeLine(new String[] { Long.toString(tid) }));
			writer.close();

		} catch (Exception e) {
			logger.error("Error extracting ids from " + fileName, e);
		}
		return null;
	}

	public static List<TwitterUser> readUsers(String fname, boolean removeTids) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			List<TwitterUser> users = mapper.readValue(new File(fname), new TypeReference<List<TwitterUser>>() {
			});
			if (removeTids)
				users.forEach(user -> user.tids = null);
			return users;
		} catch (Exception e) {
			logger.error("Failed to load users.", e);
			return null;
		}
	}

	public static List<TwitterUser> readUsers(String basePath, TestCase testCase) {
		return readUsers(basePath + "//" + testCase.toString() + ".users.users.labeled.txt", false);
	}

	public static void main(String... args) throws Exception {
		NLPUtilsInitializer.initialize("E:/authorship-cyb/nlps/");
		constructDoc(WS_DIR,
				NLPUtilsInitializer.global_twitter_model_path + "/model.ritter_ptb_alldata_fixed.20130723");
		// stat("F:\\authorship4\\ICWSM2012-v3\\",
		// "F:\\authorship4\\ICWSM2012-v3-stat\\");
	}

	public static void extract(String basePath) throws Exception {
		ArrayList<File> files = DmasFileOperations.select(basePath, Pattern.compile("\\.users$"));
		files.forEach(file -> {
			extractIds(file.getAbsolutePath());
		});
	}

	public static void constructDoc(String basePath, String taggerModel) throws Exception {

		ArrayList<File> files = DmasFileOperations.select(basePath, Pattern.compile("\\.users.tids.txt.log$"));
		Preprocessor preprocessor = new Preprocessor(Preprocessor.F_ToLowerCase(), Preprocessor.F_RemoveEtraSpace(),
				Preprocessor.F_ReplaceToken(StringResources.REGEX_URL, "URL"));

		Tagger tagger = new Tagger();
		tagger.loadModel(taggerModel);

		files.forEach(file -> {

			HashMap<Long, String> twMap = new HashMap<>();

			try {
				Lines lines = Lines.fromFile(file.getAbsolutePath());
				for (String line : lines) {
					String[] parts = line.split("\t");
					Long tid = Long.parseLong(parts[6]);
					String tw = parts[8].trim();
					if (tw.equalsIgnoreCase("null"))
						continue;
					// if (tw.startsWith("RT:") || tw.startsWith("rt:"))
					// continue;
					twMap.put(tid, tw);
				}

				String userFile = file.getAbsolutePath().replace(".users.tids.txt.log", ".users.labeled.txt");
				List<TwitterUser> users = readUsers(userFile, false);
				Stream<Document> docs = users.stream().map(user -> {
					List<String> tws = user.tids.stream().map(tid -> twMap.get(tid)).filter(MathUtilities.notNull)
							.collect(Collectors.toList());
					if (tws.size() < 1) {
						logger.info("No tweets found for user {}; skipping. ", user.uid);
						return null;
					}
					Document doc = new Document();
					doc.id = Long.toString(user.uid);
					tws.stream()//
							.filter(tw -> !tw.startsWith("RT:") && !tw.startsWith("rt:"))
							.map(tw -> preprocessor.pass(tw)).forEach(tw -> {
								try {
									List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(tw);
									List<String> wds = taggedTokens.stream().map(tt -> tt.token)
											.collect(Collectors.toList());
									List<String> tgs = taggedTokens.stream().map(tt -> tt.tag)
											.collect(Collectors.toList());
									Sentence wSentence = new Sentence();
									wSentence.tokens = wds.toArray(new String[wds.size()]);
									Sentence tSentence = new Sentence();
									tSentence.tokens = tgs.toArray(new String[tgs.size()]);
									if (wSentence.tokens.length > 0) {
										doc.sentences.add(wSentence);
										doc.sentences_tags.add(tSentence);
									}
								} catch (Exception e) {
									logger.info("Error parsing tw {}; skipping. ", tw);
								}
							});

					doc.rawContent = StringResources.JOINER_LINE.join(tws);

					if (doc.sentences.size() < 1) {
						logger.info("No tweets found for user {}; skipping. ", user.uid);
						return null;
					}
					return doc;
				}).filter(MathUtilities.notNull);

				Document.writeToFile(new StreamIterable<>(docs),
						new File(file.getAbsolutePath().replace(".users.tids.txt.log", ".users.docs")));

			} catch (Exception e) {
				logger.error("Failed to load the tweets. ", e);
			}

		});
	}

	public static class TweetUtils {

		Preprocessor preprocessor;
		Tagger tagger;

		public TweetUtils(String taggerModel) {
			preprocessor = new Preprocessor(Preprocessor.F_ToLowerCase(), Preprocessor.F_RemoveEtraSpace(),
					Preprocessor.F_ReplaceToken(StringResources.REGEX_URL, "URL"));
			tagger = new Tagger();
			try {
				tagger.loadModel(taggerModel);
			} catch (IOException e) {
				logger.error("Failed to load twitter model.", e);
			}
		}

		public Document processTweetsAsADoc(List<String> status, String doc_id) {
			Document doc = new Document();
			doc.id = doc_id;
			status.stream()//
					.filter(tw -> !tw.startsWith("RT:") && !tw.startsWith("rt:")).map(tw -> preprocessor.pass(tw))
					.forEach(tw -> {
						try {
							List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(tw);
							List<String> wds = taggedTokens.stream().map(tt -> tt.token).collect(Collectors.toList());
							List<String> tgs = taggedTokens.stream().map(tt -> tt.tag).collect(Collectors.toList());
							Sentence wSentence = new Sentence();
							wSentence.tokens = wds.toArray(new String[wds.size()]);
							Sentence tSentence = new Sentence();
							tSentence.tokens = tgs.toArray(new String[tgs.size()]);
							if (wSentence.tokens.length > 0) {
								doc.sentences.add(wSentence);
								doc.sentences_tags.add(tSentence);
							}
						} catch (Exception e) {
							logger.info("Error parsing tw {}; skipping. ", tw);
						}
					});

			doc.rawContent = StringResources.JOINER_LINE.join(status);
			return doc;
		}
	}

	public static void fetch(String basePath) throws Exception {
		ArrayList<File> files = DmasFileOperations.select(basePath, Pattern.compile("\\.users.tids.txt$"));

		files.forEach(file -> {
			try {
				TweetCorpusDownloader.downloadIdsMultiThread(file.getAbsolutePath(), file.getAbsoluteFile() + ".log",
						false, 3);
			} catch (Exception e) {
				logger.error("Failed to fetch tweets.", e);
			}
		});
	}

	public enum TestCase {
		gender, political, age
	}

	public static Iterable<Document> loadDocs(String basePath, TestCase testCase) {
		return Document.loadFromFile(new File(basePath + "//" + testCase.toString() + ".users.users.docs"));
	}

	public static void stat(String basePath, String outputPath) throws Exception {
		LineSequenceWriter writer = Lines.getLineWriter(outputPath + "//twitter_length_all.txt", false);

		Arrays.stream(TestCase.values()).forEach(tc -> {
			HashMap<String, Integer> label_user = new HashMap<>();
			HashMap<String, Integer> label_twitter = new HashMap<>();
			HashMap<String, Integer> label_token = new HashMap<>();

			List<TwitterUser> users = readUsers(basePath, tc);

			Map<String, Document> docMap = StreamSupport.stream(loadDocs(basePath, tc).spliterator(), false)
					.collect(Collectors.toMap(doc -> doc.id, doc -> doc));

			users.forEach(user -> {
				label_user.compute(user.labels.get(0), (k, v) -> v == null ? 1 : v + 1);
				Document doc = docMap.get(Long.toString(user.uid));
				if (doc == null)
					return;
				label_twitter.compute(user.labels.get(0),
						(k, v) -> v == null ? doc.sentences.size() : v + doc.sentences.size());
				Counter sum = Counter.zero();
				for (Sentence sentence : doc) {
					sum.inc(sentence.tokens.length);
					writer.writeLineNoExcept(Integer.toString(sentence.tokens.length));
				}
				label_token.compute(user.labels.get(0), (k, v) -> v == null ? sum.getVal() : v + sum.getVal());
			});
			System.out.println(tc.toString());
			System.out.println(label_user.toString());
			System.out.println(label_twitter.toString());
			System.out.println(label_token.toString());
		});
		writer.close();

	}

}
