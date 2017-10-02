/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.DmasApplication;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public abstract class Sentences implements Iterable<Sentence> {

	private static Logger logger = LoggerFactory.getLogger(Sentences.class);

	public static Sentences fromLines(Lines lines) {
		return new SentencesFromLines(lines);
	}

	public static Sentences fromLines(Lines rawlines, Tokenizer tokenizer,
			Parser parser) {
		return new SentencesFromLines(rawlines, tokenizer, parser);
	}

	public static Sentences fromLines(Lines rawlines, Tokenizer tokenizer) {
		return new SentencesFromLines(rawlines, tokenizer);
	}

	public static Sentences fromParagraphs(Paragraphs paragraphs) {
		return new SentencesFromParagraphs(paragraphs);
	}

	public static boolean flushToFile(Sentences sentences, String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);

		File file = new File(fileToSave);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for (Sentence sentence : sentences) {
				bw.write(sentence.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToFile(Sentences sentences, String fileToSave,
			Charset charset) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);

		File file = new File(fileToSave);
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), charset));
			for (Sentence sentence : sentences) {
				bw.write(sentence.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToGzip(Sentences sentences, String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8);
			BufferedWriter bw = new BufferedWriter(writer);

			for (Sentence sentence : sentences) {
				bw.write(sentence.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();

		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToGzip(Sentences sentences, String fileToSave,
			Charset charset) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			Writer writer = new OutputStreamWriter(outputStream, charset);
			BufferedWriter bw = new BufferedWriter(writer);

			for (Sentence sentence : sentences) {
				bw.write(sentence.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();

		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public void print() {
		for (Sentence sentence : this) {
			System.out.println(sentence.toString());
		}
	}

	public static interface SentenceFilter {
		boolean validate(Sentence sentence);
	}

	static class SentenceLengthFilter implements SentenceFilter {

		public SentenceLengthFilter(int minimum_lenght) {
			this.minimum_lenght = minimum_lenght;
		}

		int minimum_lenght = Integer.MAX_VALUE;

		@Override
		public boolean validate(Sentence sentence) {
			if (sentence == null || sentence.tokens == null
					|| sentence.tokens.length < minimum_lenght)
				return false;
			return true;
		}

	}

	static class SentenceSkipEmptyFilter implements SentenceFilter {

		@Override
		public boolean validate(Sentence sentence) {
			if (sentence == null || sentence.tokens == null)
				return false;
			return true;
		}

	}

	public static SentenceFilter filterLength(int minimunLength) {
		return new SentenceLengthFilter(minimunLength);
	}

	public static SentenceFilter filterSkipEmpty() {
		return new SentenceSkipEmptyFilter();
	}

	public static SentencesFiltered filter(Sentences sentences,
			SentenceFilter... filters) {
		return new SentencesFiltered(sentences, filters);
	}
}
