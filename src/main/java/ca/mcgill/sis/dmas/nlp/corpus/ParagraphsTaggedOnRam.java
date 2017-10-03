/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.DmasApplication;
import ca.mcgill.sis.dmas.nlp.corpus.ParagraphTagged.pt_deserialize_unit;

public class ParagraphsTaggedOnRam extends ParagraphsTagged {

	private static Logger logger = LoggerFactory
			.getLogger(ParagraphsTaggedOnRam.class);

	ArrayList<ParagraphTagged> data = new ArrayList<>();
	
	public ParagraphsTaggedOnRam(List<ParagraphTagged> pts){
		data.addAll(pts);
	}

	public ParagraphsTaggedOnRam(String file, boolean isGzip) {
		file = DmasApplication.applyDataContext(file);
		Iterator<ParagraphTagged.pt_deserialize_unit> l_ite_reader = null;
		BufferedReader bReader = null;
		try {
			if (isGzip) {
				InputStream fileStream = new FileInputStream(new File(file));
				InputStream gzipStream = new GZIPInputStream(fileStream);
				Reader decoder = new InputStreamReader(gzipStream,
						Charsets.UTF_8);
				bReader = new BufferedReader(decoder);
				l_ite_reader = (new ObjectMapper()).reader(
						ParagraphTagged.pt_deserialize_unit.class).readValues(
						bReader);
			} else {
				bReader = new BufferedReader(new FileReader(new File(file)));
				l_ite_reader = (new ObjectMapper()).reader(
						ParagraphTagged.pt_deserialize_unit.class).readValues(
						bReader);
			}
			while (l_ite_reader.hasNext()) {
				pt_deserialize_unit var = l_ite_reader.next();
				if(var != null)
					data.add(var.toParagraphTagged());
			}
			if (bReader != null)
				bReader.close();
		} catch (Exception e) {
			logger.error("Failed to read file", e);
			return;
		}
	}

	@Override
	public Iterator<ParagraphTagged> iterator() {
		return data.iterator();
	}
}
