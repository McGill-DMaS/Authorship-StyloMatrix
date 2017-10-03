/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.DmasApplication;
import ca.mcgill.sis.dmas.env.StringResources;

public class LineSequenceWriter {

	BufferedWriter bw;

	public LineSequenceWriter(String fileName, boolean compressed, Charset charset) throws Exception {
		fileName = DmasApplication.applyDataContext(fileName);
		if (!compressed) {
			FileOutputStream fileOutputStream = new FileOutputStream(fileName);
			bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream, charset));
		} else {
			FileOutputStream fileOutputStream = new FileOutputStream(fileName);
			GZIPOutputStream outputStream = new GZIPOutputStream(fileOutputStream);
			bw = new BufferedWriter(new OutputStreamWriter(outputStream, charset));
		}
	}

	public void close() throws Exception {
		bw.close();
	}

	public void writeLine(String line) throws Exception {
		bw.write(line);
		bw.write(StringResources.STR_LINEBREAK);
	}

	public void writeLine(String... tokens) {
		try {
			bw.write(StringResources.JOINER_TOKEN.join(tokens));
			bw.write(StringResources.STR_LINEBREAK);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeLineCSV(Object... partsForOneLine) throws Exception {
		bw.write(StringResources.JOINER_TOKEN_CSV.join(partsForOneLine));
		bw.write(StringResources.STR_LINEBREAK);
	}

	public void writeLine(String line, boolean flush) throws Exception {
		bw.write(line);
		if (flush)
			bw.flush();
		bw.write(StringResources.STR_LINEBREAK);
	}

	public void writeLineNoExcept(String line) {
		try {
			bw.write(line);
			bw.write(StringResources.STR_LINEBREAK);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeLineNoExcept(String line, boolean flush) {
		try {
			bw.write(line);
			bw.write(StringResources.STR_LINEBREAK);
			if (flush)
				bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeLineCSV(boolean flush, Object... partsForOneLine) throws Exception {
		bw.write(StringResources.JOINER_TOKEN_CSV.join(partsForOneLine));
		bw.write(StringResources.STR_LINEBREAK);
		if (flush)
			bw.flush();
	}

}
