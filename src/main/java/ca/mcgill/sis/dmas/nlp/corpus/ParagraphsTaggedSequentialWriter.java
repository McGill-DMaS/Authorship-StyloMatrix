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
import java.util.zip.GZIPOutputStream;

import ca.mcgill.sis.dmas.env.DmasApplication;
import ca.mcgill.sis.dmas.env.StringResources;

import com.google.common.base.Charsets;

public class ParagraphsTaggedSequentialWriter {
	BufferedWriter bw;

	public ParagraphsTaggedSequentialWriter(String fileName, boolean compressed)
			throws Exception {
		fileName = DmasApplication.applyDataContext(fileName);
		if (!compressed) {
			bw = new BufferedWriter(new FileWriter(new File(fileName)));
		} else {
			FileOutputStream fileOutputStream = new FileOutputStream(fileName);
			GZIPOutputStream outputStream = new GZIPOutputStream(
					fileOutputStream);
			bw = new BufferedWriter(new OutputStreamWriter(outputStream, Charsets.UTF_8));
		}
	}

	public void close() throws Exception {
		bw.close();
	}

	public void writeLine(ParagraphTagged paragraphTagged) throws Exception {
		bw.write(paragraphTagged.serializeToJason());
		bw.write(StringResources.STR_LINEBREAK);
	}
}
