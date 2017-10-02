/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ca.mcgill.sis.dmas.nlp.corpus.parser.Punctuation;

import com.google.common.collect.ImmutableMap.Builder;

public class FilterSeperatePuntuationInToken implements Filter {
	ImmutableMap<Pattern, String> patterns;

	public FilterSeperatePuntuationInToken() {

		Builder<Pattern, String> builder = ImmutableMap.<Pattern, String> builder();

		for (String element : Punctuation.characters) {
			builder.put(Pattern.compile(Pattern.quote(element), Pattern.MULTILINE),
					Matcher.quoteReplacement(" " + element + " "));
		}

		patterns = builder.build();
	}

	@Override
	public String pass(String line) {
		for (Entry<Pattern, String> pattern : patterns.entrySet()) {
			line = pattern.getKey().matcher(line).replaceAll(pattern.getValue());
		}
		return line;
	}
}
