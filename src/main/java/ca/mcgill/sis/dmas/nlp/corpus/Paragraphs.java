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
import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public abstract class Paragraphs implements Iterable<Paragraph> {

	private static Logger logger = LoggerFactory.getLogger(Paragraphs.class);

	public static Paragraphs fromLines(Lines lines,
			String paragraphSeperatePattern) {
		return new ParagraphsFromLines(lines, paragraphSeperatePattern);
	}

	public static Paragraphs fromLines(Lines lines, SentenceDetector detector,
			Tokenizer tokenizer) {
		return new ParagraphsFromLines(lines, detector, tokenizer);
	}

	public static Paragraphs fromLines(Lines lines, SentenceDetector detector,
			Tokenizer tokenizer, Parser parser) {
		return new ParagraphsFromLines(lines, detector, tokenizer, parser);
	}

	public static Paragraphs fromParagraphsTagged(
			ParagraphsTagged paragraphsTagged) {
		return new ParagraphsFromParagraphsTagged(paragraphsTagged);
	}

	public static Paragraphs fromFile(String fileName, boolean isGzip) {
		return new ParagraphsFromFile(fileName, isGzip);
	}

	public static Paragraphs merge(Paragraphs... paragraphs) {
		return new ParagraphsMerged(paragraphs);
	}

	public static Paragraphs skip(Paragraphs paragraphs, int numToSkip) {
		return new ParagraphsSkip(paragraphs, numToSkip);
	}

	public static Paragraphs limit(Paragraphs paragraphs, int limitedSteps) {
		return new ParagraphsLimit(paragraphs, limitedSteps);
	}

	public static boolean flushToFile(Iterable<Paragraph> paragraphs,
			String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);

		File file = new File(fileToSave);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for (Paragraph paragraph : paragraphs) {
				bw.write(paragraph.serializeToJason());
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToFile(Iterable<Paragraph> paragraphs,
			String fileToSave, Charset charset) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);

		File file = new File(fileToSave);
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), charset));
			for (Paragraph paragraph : paragraphs) {
				bw.write(paragraph.serializeToJason());
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToGzip(Iterable<Paragraph> paragraphs,
			String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8);
			BufferedWriter bw = new BufferedWriter(writer);

			for (Paragraph paragraph : paragraphs) {
				bw.write(paragraph.serializeToJason());
			}
			bw.flush();
			bw.close();

		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public static boolean flushToGzip(Iterable<Paragraph> paragraphs,
			String fileToSave, Charset charset) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			Writer writer = new OutputStreamWriter(outputStream, charset);
			BufferedWriter bw = new BufferedWriter(writer);

			for (Paragraph paragraph : paragraphs) {
				bw.write(paragraph.serializeToJason());
			}
			bw.flush();
			bw.close();

		} catch (Exception e) {
			logger.error("Failed to save into file.", e);
			return false;
		}
		return true;
	}

	public Sentences toSentences() {
		return new SentencesFromParagraphs(this);
	}

	public void print() {
		for (Paragraph paragraph : this) {
			paragraph.print();
			System.out.println(StringResources.STR_PARAGRAPHBREAK);
		}
	}
}
