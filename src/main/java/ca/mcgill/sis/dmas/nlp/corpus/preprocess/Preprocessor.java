/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

public class Preprocessor {

	public static String preprocess(String line, Filter... filters) {
		if (filters == null || filters.length < 1)
			return line;

		for (Filter filter : filters) {
			line = filter.pass(line);
		}
		return line;
	}

	public Preprocessor(Filter... localFilters) {
		this.local_filters = localFilters;
	}

	Filter[] local_filters;

	public String pass(String line) {
		if (local_filters == null || local_filters.length < 1)
			return line;

		for (Filter filter : local_filters) {
			line = filter.pass(line);
		}
		return line;
	}

	public static Filter F_AphaDigitOnly() {
		return new FilterAlphaDigitOnly();
	}

	public static Filter F_RemoveURL() {
		return new FilterRemoveURL();
	}

	public static Filter F_RemoveNonASCII() {
		return new FilterRemoveNonASCII();
	}

	public static Filter F_RemoveEtraSpace() {
		return new FilterRemoveExtraSapce();
	}

	public static Filter F_ToLowerCase() {
		return new FilterToCaseLower();
	}

	public static Filter F_ToUpperCase() {
		return new FilterToCaseUpper();
	}

	public static Filter F_ReplaceToken(String... pattern) {
		return FilterReplaceToken.of(pattern);
	}

	public static Filter F_ExtractFromHTML() {
		return new FilterExtractTextFromHTML();
	}

	public static Filter F_SeperatePunctuation() {
		return new FilterSeperatePuntuationInToken();
	}

	// public static void preprocessSentences(Lines lines, Filter[] filters,
	// Tokenizer tokenizer, String outputFile) throws Exception {
	// outputFile = DmasApplication.applyDataContext(outputFile);
	// File outfile = new File(outputFile);
	// FileWriter fw = new FileWriter(outfile.getAbsoluteFile());
	// BufferedWriter bw = new BufferedWriter(fw);
	// Joiner joiner = StringResources.JOINER_TOKEN;
	// logger.info("Start pre-processing.. Saving into file {}",
	// outfile.getAbsolutePath());
	// long count = 0;
	// long localCount = 0;
	// for (String line : lines) {
	// Sentence sent = new Sentence(line.toLowerCase(), tokenizer);
	// if (filters != null)
	// for (Filter filter : filters) {
	// sent = filter.pass(sent);
	// }
	// String newline = joiner.join(sent.tokens);
	// bw.append(newline).append(StringResources.STR_LINEBREAK);
	// count++;
	// localCount++;
	// if (localCount > 50000) {
	// logger.info("Processed {} lines", count);
	// localCount = 0;
	// }
	// }
	// logger.info("Finished pre-processing.. Total sentences: {}", count);
	// bw.close();
	// }
	//
	// public static void preprocessParagraphes(Lines paragraphs,
	// Filter[] filters, Tokenizer tokenizer, SentenceDetector detector,
	// String outputFile) throws Exception {
	// outputFile = DmasApplication.applyDataContext(outputFile);
	// File outfile = new File(outputFile);
	// FileWriter fw = new FileWriter(outfile.getAbsoluteFile());
	// BufferedWriter bw = new BufferedWriter(fw);
	// Joiner joiner = StringResources.JOINER_TOKEN;
	// logger.info("Start pre-processing.. Saving into file {}",
	// outfile.getAbsolutePath());
	// long count = 0;
	// long localCount = 0;
	// for (String line : paragraphs) {
	// String[] raw_sents = detector.detectSentences(line);
	// for (String raw_sent : raw_sents) {
	// Sentence sent = new Sentence(raw_sent, tokenizer);
	// if (filters != null)
	// for (Filter filter : filters) {
	// sent = filter.pass(sent);
	// }
	// String newline = joiner.join(sent.tokens);
	// bw.append(newline).append(StringResources.STR_LINEBREAK);
	// count++;
	// localCount++;
	// if (localCount > 50000) {
	// logger.info("Processed {} lines", count);
	// localCount = 0;
	// }
	// }
	// if (raw_sents.length > 0) {
	// bw.append(StringResources.STR_PARAGRAPHBREAK).append(
	// StringResources.STR_LINEBREAK);
	// }
	// }
	// logger.info("Finished pre-processing.. Total sentences: {}", count);
	// bw.close();
	// }
	//
	// public static void preprocessParagraphes(LinesSet setOfParagraphs,
	// Filter[] filters, Tokenizer tokenizer, SentenceDetector detector,
	// String outputFile) throws Exception {
	// outputFile = DmasApplication.applyDataContext(outputFile);
	// File outfile = new File(outputFile);
	// FileWriter fw = new FileWriter(outfile.getAbsoluteFile());
	// BufferedWriter bw = new BufferedWriter(fw);
	// Joiner joiner = StringResources.JOINER_TOKEN;
	// logger.info("Start pre-processing.. Saving into file {}",
	// outfile.getAbsolutePath());
	// long count = 0;
	// long localCount = 0;
	//
	// for(Lines paragraphs : setOfParagraphs){
	// for (String line : paragraphs) {
	// String[] raw_sents = detector.detectSentences(line);
	// for (String raw_sent : raw_sents) {
	// Sentence sent = new Sentence(raw_sent, tokenizer);
	// if (filters != null)
	// for (Filter filter : filters) {
	// sent = filter.pass(sent);
	// }
	// String newline = joiner.join(sent.tokens);
	// bw.append(newline).append(StringResources.STR_LINEBREAK);
	// count++;
	// localCount++;
	// if (localCount > 50000) {
	// logger.info("Processed {} lines", count);
	// localCount = 0;
	// }
	// }
	// if (raw_sents.length > 0) {
	// bw.append(StringResources.STR_PARAGRAPHBREAK).append(
	// StringResources.STR_LINEBREAK);
	// }
	// }
	// }
	// logger.info("Finished pre-processing.. Total sentences: {}", count);
	// bw.close();
	// }

}
