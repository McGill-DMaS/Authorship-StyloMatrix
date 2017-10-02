/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.Iterator;
import java.util.regex.Pattern;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.Lines;

public class SentencesFiltered extends Sentences{
	
	SentenceFilter[] filters = null;
	Sentences sentences;
	public SentencesFiltered(Sentences sentences, SentenceFilter ... filters){
		this.filters = filters;;
		this.sentences = sentences;
	}

	@Override
	public Iterator<Sentence> iterator() {
		return new SentencesFilteredIterator();
	}
	
	private class SentencesFilteredIterator implements Iterator<Sentence>{
		
		Iterator<Sentence> ite = sentences.iterator();
		Sentence thisline = null;
		
		boolean allValid(Sentence sentence){
			if(filters == null)
				return true;
			for (SentenceFilter filter : filters) {
				if(!filter.validate(sentence))
					return false;
			}
			return true;
		}
		
		@Override
		public boolean hasNext() {
			while (thisline == null || !allValid(thisline)) {
				if(ite.hasNext())
					thisline = ite.next();
				else
					break;
			}
			if(thisline == null || !allValid(thisline))
				return false;
			else
				return true;
		}

		@Override
		public Sentence next() {
			Sentence returnLine = thisline;
			thisline = null;
			return returnLine;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static void main(String [] args){
		Lines lines = Lines.from("asd fasd fas df","asdf sdf=== sdcs erwer","sdf sdf","w ecserwer===asdf", "asd fasd fse rwer", "== === === ==", "");
		Sentences sentences = Sentences.fromLines(lines);
		sentences = new SentencesFiltered(sentences, Sentences.filterLength(3), Sentences.filterSkipEmpty());
		sentences.print();
	}

}
