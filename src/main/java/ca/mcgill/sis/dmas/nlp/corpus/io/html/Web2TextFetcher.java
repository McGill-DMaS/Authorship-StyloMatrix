/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus.io.html;

import java.io.StringReader;

import org.xml.sax.InputSource;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tokenizer;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

public class Web2TextFetcher {

	public static String getText(String html) throws Exception {

		TextDocument doc = new BoilerpipeSAXInput(new InputSource(new StringReader(html))).getTextDocument();

		ArticleExtractor.INSTANCE.process(doc);

		// iterate over all blocks (= segments as "ArticleExtractor" sees them)
		StringBuilder sBuilder = new StringBuilder();
		for (TextBlock block : doc.getTextBlocks()) {
			sBuilder.append(block.getText()).append(StringResources.STR_TOKENBREAK);
		}

		String content = sBuilder.toString();

		return sBuilder.toString();
	}
}
