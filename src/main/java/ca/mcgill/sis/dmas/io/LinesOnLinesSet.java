/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io;

import java.util.Iterator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinesOnLinesSet extends Lines{

	private static Logger logger = LoggerFactory.getLogger(LinesOnLinesSet.class);
	
	LinesSet set;
	
	public LinesOnLinesSet(LinesSet set){
		this.set = set;
	}
	
	@Override
	public Iterator<String> iterator() {
		return new LinesOnLinesSetIterator();
	}
	
	public class LinesOnLinesSetIterator implements Iterator<String>{

		Iterator<Lines> ite = set.iterator();
		Iterator<String> current = null;
		
		@Override
		public boolean hasNext() {
			if(current != null && current.hasNext())
				return true;
			
			while (ite.hasNext()) {
				current = ite.next().iterator();
				if(current.hasNext())
					return true;
			}
			return false;
		}

		@Override
		public String next() {
			return current.next();
		}

		@Override
		public void remove() {
			logger.error("Unsupport operation: remove element");
		}
	}

}
