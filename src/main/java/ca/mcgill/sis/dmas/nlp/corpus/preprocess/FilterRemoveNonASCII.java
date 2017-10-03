/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

import java.util.regex.Pattern;

import ca.mcgill.sis.dmas.env.StringResources;

public class FilterRemoveNonASCII implements Filter {

	
	Pattern pattern = Pattern.compile(StringResources.REGEX_NON_ASCII);

	
	@Override
	public String pass(String line) {
		return pattern.matcher(line).replaceAll("");
	}

}
