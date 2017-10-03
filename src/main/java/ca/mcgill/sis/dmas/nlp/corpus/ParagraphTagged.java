/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.nlp.corpus.parser.SentenceDetector;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;

public class ParagraphTagged extends Paragraph {


	public ParagraphTagged(String rawText, SentenceDetector detector,
			Tokenizer tokenizer) {
		super(rawText, detector, tokenizer);

	}
	
	public ParagraphTagged(Sentence[] sentences_) {
		super(sentences_);

	}
	
	@Override
	public String toString(){
		return Arrays.toString(sentences);
	}
	
	public ParagraphTagged(Paragraph p, ArrayList<String> tags){
		super(p.sentences);
		this.Tags = tags;
	}
	
	public ParagraphTagged(){}

	public ArrayList<String> Tags = new ArrayList<>();
	
	@JsonIgnore
	public Object Tag;
	
	public static class pt_deserialize_unit{
		public ArrayList<String> s = new ArrayList<>();
		public String [] t;
		
		public ParagraphTagged toParagraphTagged(){
			ParagraphTagged pt = new ParagraphTagged();
			pt.sentences = Sentence.convert(this.s);
			for (String tag : this.t) {
				pt.Tags.add(tag);
			}
			return pt;
		}
	}
	
	public String serializeToJason() throws Exception{
		pt_deserialize_unit u = new pt_deserialize_unit();
		for (Sentence sentence : sentences) {
			u.s.add(sentence.toString());
		}
		u.t = this.Tags.toArray(new String[Tags.size()]);
		return mapper.writeValueAsString(u);
	}
	
	static ObjectMapper mapper = new ObjectMapper();
	
	
	public static ParagraphTagged deserializeFromJason(String str)throws Exception{
		pt_deserialize_unit u = mapper.readValue(str, pt_deserialize_unit.class);
		return u.toParagraphTagged();
	}

}
