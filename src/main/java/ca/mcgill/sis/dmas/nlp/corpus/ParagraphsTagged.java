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
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.DmasApplication;

public abstract class ParagraphsTagged implements Iterable<ParagraphTagged> {

	private static Logger logger = LoggerFactory
			.getLogger(ParagraphsTagged.class);

	public ParagraphsTagged() {
	}
	
	public static ParagraphsTagged from(ParagraphTagged... paragraphs) {
		return new ParagraphsTaggedOnRam(Arrays.asList(paragraphs));
	}

	public static ParagraphsTagged fromFile(String file, boolean isGizp) {
		return new ParagraphsTaggedFromFile(file, isGizp);
	}

	public static ParagraphsTagged fromFile(String file, boolean isGizp,
			boolean cached) {
		if (cached)
			return new ParagraphsTaggedOnRam(file, isGizp);
		else
			return new ParagraphsTaggedFromFile(file, isGizp);
	}

	public static ParagraphsTagged merge(ParagraphsTagged... paragraphs) {
		return new ParagraphsTaggedMerged(paragraphs);
	}

	public static ParagraphsTagged skip(ParagraphsTagged paragraphs,
			int numToSkip) {
		return new ParagraphsTaggedSkip(paragraphs, numToSkip);
	}

	public static ParagraphsTagged limit(ParagraphsTagged paragraphs,
			int limitedSteps) {
		return new ParagraphsTaggedLimit(paragraphs, limitedSteps);
	}

	public static ParagraphsTaggedSequentialWriter getWriter(String fileName,
			boolean compressed) throws Exception {
		return new ParagraphsTaggedSequentialWriter(fileName, compressed);
	}

	public static void flushToFile(Iterable<ParagraphTagged> pts,
			String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for (ParagraphTagged pt : pts) {
				bw.write(pt.serializeToJason());
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save file..");
		}
	}

	public static void flushToGzip(Iterable<ParagraphTagged> pts,
			String fileToSave) {
		fileToSave = DmasApplication.applyDataContext(fileToSave);
		File file = new File(fileToSave);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8);
			BufferedWriter bw = new BufferedWriter(writer);

			for (ParagraphTagged pt : pts) {
				bw.write(pt.serializeToJason());
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			logger.error("Failed to save file..");
		}
	}

}
