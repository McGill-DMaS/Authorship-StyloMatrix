/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.preprocess;

import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ca.mcgill.sis.dmas.env.StringResources;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

public class FilterExtractTextFromHTML implements Filter{
	
	private static Logger logger = LoggerFactory.getLogger(FilterExtractTextFromHTML.class);

	@Override
	public String pass(String line) {
		
		TextDocument doc;
		try {
			doc = new BoilerpipeSAXInput(new InputSource(
			        new StringReader(line))).getTextDocument();
			ArticleExtractor.INSTANCE.process(doc);
		} catch (Exception e) {
			logger.error("Failed to parse line: {}", line);
			return StringResources.STR_EMPTY;
		}
		
		// iterate over all blocks (= segments as "ArticleExtractor" sees them)
		StringBuilder sBuilder = new StringBuilder();
		for (TextBlock block : doc.getTextBlocks()) {
			sBuilder.append(block.getText()).append(StringResources.STR_TOKENBREAK);
		}
		
		String content = sBuilder.toString();
		
		return content;
	}

}
