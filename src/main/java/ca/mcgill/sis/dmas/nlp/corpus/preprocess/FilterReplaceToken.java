/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class FilterReplaceToken implements Filter {
	private static Logger logger = LoggerFactory
			.getLogger(FilterReplaceToken.class);

	ImmutableMap<Pattern, String> patterns;

	public FilterReplaceToken(ImmutableMap<String, String> map) {

		Builder<Pattern, String> builder = ImmutableMap
				.<Pattern, String> builder();

		for (Entry<String, String> element : map.entrySet()) {
			builder.put(Pattern.compile(element.getKey(), Pattern.MULTILINE ),
					element.getValue());
		}

		patterns = builder.build();
	}

	public static FilterReplaceToken of(String... patterns) {

		if (patterns.length % 2 != 0) {
			logger.error(
					"The input number of string is {} which is not even number.",
					patterns.length);
			return null;
		}

		com.google.common.collect.ImmutableMap.Builder<String, String> builder = ImmutableMap
				.<String, String> builder();

		for (int i = 0; i < patterns.length; i += 2) {
			builder.put(patterns[i], patterns[i + 1]);
		}

		return new FilterReplaceToken(builder.build());
	}

	@Override
	public String pass(String line) {
		for (Entry<Pattern, String> pattern : patterns.entrySet()) {
			line = pattern.getKey().matcher(line)
					.replaceAll(pattern.getValue());
		}
		return line;
	}
}
