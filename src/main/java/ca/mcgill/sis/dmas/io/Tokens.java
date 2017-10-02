/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io;

import java.nio.charset.Charset;

public abstract class Tokens implements Iterable<String> {

	
	public static Tokens fromFile(String fileName) throws Exception{
		return new TokensOnDisk(fileName);
	}
	
	public static Tokens fromFile(String fileName, String deliminator) throws Exception{
		return new TokensOnDisk(fileName, deliminator);
	}
	
	
	public static Tokens fromGzip(String fileName) throws Exception{
		return new TokensOnGzip(fileName);
	}
	
	public static Tokens fromGzip(String fileName, String deliminator, Charset charset) throws Exception{
		return new TokensOnGzip(fileName, deliminator, charset);
	}
	
	public abstract Lines groupIntoLines(int lineLength);
}
