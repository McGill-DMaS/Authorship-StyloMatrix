/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.parser;


public class Symbols {
	public static boolean is(String token){
		token = token.trim();
		if(token.length() > 1)
			return false;
		return !Character.isLetterOrDigit(token.charAt(0));
	}
	
}
