/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Parser;
import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public class Paragraph extends Sentences {
	
	private static Logger logger = LoggerFactory.getLogger(Paragraph.class);
	
	public Sentence [] sentences;
	private Integer id = null;
	
	public int getID(){
		if(id != null)
			return id;
		else {
			id = StringResources.JOINER_LINE.join(sentences).hashCode();
			return id;
		}
	}
	
	public void setID(int id){
		this.id = id;
	}
	
	public Paragraph(Sentence [] sentences_){
		sentences = sentences_;
	}
	
	public Paragraph(String rawText, SentenceDetector detector, Tokenizer tokenizer){
		String [] raw_sentences = detector.detectSentences(rawText);
		sentences = new Sentence[raw_sentences.length];
		for(int i = 0; i < raw_sentences.length; ++i){
			sentences[i] = new Sentence(raw_sentences[i], tokenizer);
		}
	}
	
	public Paragraph(String rawText, SentenceDetector detector, Tokenizer tokenizer, Parser parser){
		String [] raw_sentences = detector.detectSentences(rawText);
		sentences = new Sentence[raw_sentences.length];
		for(int i = 0; i < raw_sentences.length; ++i){
			sentences[i] = new Sentence(raw_sentences[i], tokenizer, parser);
		}
	}
	
	public static class P_serialization_unit{
		public ArrayList<String> s = new ArrayList<>();
		
		public Paragraph toParagraph(){
			Paragraph pt = new Paragraph();
			pt.sentences = Sentence.convert(this.s);
			return pt;
		}
		
	}
	
	public String serializeToJason() throws Exception{
		P_serialization_unit u = new P_serialization_unit();
		for (Sentence sentence : sentences) {
			u.s.add(sentence.toString());
		}
		return mapper.writeValueAsString(u);
	}
	
	static ObjectMapper mapper = new ObjectMapper();
	
	
	public static Paragraph deserializeFromJason(String str) throws Exception{
		P_serialization_unit u = mapper.readValue(str, P_serialization_unit.class);
		return u.toParagraph();
	}
	
	public Paragraph(){}
	
	public String getFullText(){
		return StringResources.JOINER_LINE.join(sentences);
	}
	
	
	@Override
	public Iterator<Sentence> iterator() {
		return new PSIterator();
	}
	
	public class PSIterator implements Iterator<Sentence>{
		
		int index = 0;

		@Override
		public boolean hasNext() {
			if(index < sentences.length)
				return true;
			else {
				return false;
			}
		}

		@Override
		public Sentence next() {
			return sentences[index++];
		}

		@Override
		public void remove() {
			logger.error("Unsupported operation. Could not remove element.");
		}
		
	}
	
	
	
	
}
