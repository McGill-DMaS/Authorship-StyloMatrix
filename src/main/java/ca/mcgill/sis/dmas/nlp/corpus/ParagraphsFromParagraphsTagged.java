/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.Iterator;

public class ParagraphsFromParagraphsTagged extends Paragraphs {

	public ParagraphsFromParagraphsTagged(ParagraphsTagged paragraphsTagged) {
		pt = paragraphsTagged;
	}

	ParagraphsTagged pt;

	@Override
	public Iterator<Paragraph> iterator() {
		return new PFPTIte();
	}

	public class PFPTIte implements Iterator<Paragraph> {

		private Iterator<ParagraphTagged> l_ite;

		PFPTIte() {
			if (pt != null)
				l_ite = pt.iterator();
			else {
				l_ite = null;
			}
		}

		@Override
		public boolean hasNext() {
			if(l_ite != null && l_ite.hasNext())
				return true;
			else {
				return false;
			}
		}

		@Override
		public Paragraph next() {
			return l_ite.next();
		}

		@Override
		public void remove() {
			l_ite.remove();
		}

	}

}
