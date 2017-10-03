/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TaggerSpanishStandford extends Tagger {
	
	
	private MaxentTagger tagger = new MaxentTagger(NlpUtilsStandford.getTaggerSpanishModelPath());

	@Override
	public List<String> tag(String[] tokens) {
		ArrayList<HasWord> hasWords = new ArrayList<>();
		for (String token : tokens) {
			hasWords.add(new Word(token));
		}
		List<TaggedWord> tagged = tagger.tagSentence(hasWords);
		ArrayList<String> tags = new ArrayList<>();
		for(int i = 0; i < tagged.size(); ++i)
			tags.add(tagged.get(i).tag());
		return tags;
	}

}
