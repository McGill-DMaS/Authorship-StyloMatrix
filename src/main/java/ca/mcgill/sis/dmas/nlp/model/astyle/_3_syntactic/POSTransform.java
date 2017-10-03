package ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ca.mcgill.sis.dmas.io.collection.StreamIterable;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;

public class POSTransform {

	public static void generatePOS(String inFile, String outFile, Tagger tagger) throws Exception {
		Iterable<Document> docsIn = Document.loadFromFile(new File(inFile));
		Stream<Document> outStream = StreamSupport.stream(docsIn.spliterator(), false).map(docIn -> {
			docIn.sentences.forEach(sent -> {
				List<String> tags = new ArrayList<>(Arrays.asList(sent.tokens));
				tags.addAll(tagger.tag(sent.tokens));
				sent.tokens = tags.toArray(new String[tags.size()]);
			});
			return docIn;
		});
		Document.writeToFile(new StreamIterable<>(outStream), new File(outFile));
	}

	public static void generatePOSforPAN2013(String basePath) throws Exception {
		generatePOS(basePath + "//essay-test.txt", basePath + "//essay-test-pos.txt", Tagger.taggerStanFord);
		generatePOS(basePath + "//essay-training.txt", basePath + "//essay-training-pos.txt", Tagger.taggerStanFord);
		generatePOS(basePath + "//essay-verification.txt", basePath + "//essay-verification-pos.txt",
				Tagger.taggerStanFord);
		generatePOS(basePath + "//novel-test.txt", basePath + "//novel-test-pos.txt", Tagger.taggerStanFord);
		generatePOS(basePath + "//novel-training.txt", basePath + "//novel-training-pos.txt", Tagger.taggerStanFord);
		generatePOS(basePath + "//novel-verification.txt", basePath + "//novel-verification-pos.txt",
				Tagger.taggerStanFord);
	}

	public static void generatePOSforICWSM(String basePath, String taggerModelPath) throws Exception {
		Tagger tagger = Tagger.newTweetTagger(taggerModelPath);
		generatePOS(basePath + "//political.users.users.docs", basePath + "political.users.users.docs.pos", tagger);
		generatePOS(basePath + "//gender.users.users.docs", basePath + "gender.users.users.docs.pos", tagger);
		generatePOS(basePath + "//age.users.users.docs", basePath + "age.users.users.docs.pos", tagger);

	}

	public static void main(String[] args) throws Exception {
		generatePOSforPAN2013("E:\\authorship\\PAN2013AI-preprocessed-nopunctuation\\");
		generatePOSforICWSM("E:\\authorship\\ICWSM2012\\", "E:\\authorship\\model.ritter_ptb_alldata_fixed.20130723");
	}

}
