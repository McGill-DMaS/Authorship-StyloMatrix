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
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import ca.mcgill.sis.dmas.env.DmasApplication;

public class ParagraphsFromFile extends Paragraphs{

	private static Logger logger = LoggerFactory
			.getLogger(ParagraphsFromFile.class);
	private boolean isGzip;
	private File fileToRead;

	public ParagraphsFromFile(String file, boolean isGzip) {
		file = DmasApplication.applyDataContext(file);
		fileToRead = new File(file);
		this.isGzip = isGzip;
	}
	
	@Override
	public Iterator<Paragraph> iterator() {
		return new PFFIterator();
	}
	
	public class PFFIterator implements Iterator<Paragraph>{
		
		Iterator<Paragraph.P_serialization_unit> l_ite_reader;
		
		public PFFIterator(){
			try {
				if (isGzip) {
					InputStream fileStream = new FileInputStream(fileToRead);
					InputStream gzipStream = new GZIPInputStream(fileStream);
					Reader decoder = new InputStreamReader(gzipStream, Charsets.UTF_8);
					BufferedReader bReader = new BufferedReader(decoder);
					l_ite_reader = (new ObjectMapper()).reader(
							Paragraph.P_serialization_unit.class).readValues(bReader);
				} else {
					BufferedReader bReader = new BufferedReader(new FileReader(fileToRead));
					l_ite_reader = (new ObjectMapper()).reader(
							Paragraph.P_serialization_unit.class).readValues(bReader);
				}
			} catch (Exception e) {
				logger.error("Failed to load from file. Returning null. May cause error.", e);
			}
		}

		@Override
		public boolean hasNext() {
			if(l_ite_reader == null)
				return false;
			return l_ite_reader.hasNext();
		}

		@Override
		public Paragraph next() {
			Paragraph.P_serialization_unit unit = l_ite_reader.next();
			return unit.toParagraph();
		}

		@Override
		public void remove() {
			logger.error("Unsupported operation: remove");
		}
		
	}

}
