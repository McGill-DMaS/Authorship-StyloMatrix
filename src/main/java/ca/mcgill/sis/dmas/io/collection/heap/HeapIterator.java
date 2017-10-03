/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection.heap;

import java.util.Iterator;
import java.util.TreeSet;

public class HeapIterator<T> implements Iterator<HeapEntry<T>> {

	public HeapIterator(TreeSet<HeapEntry<T>> data, boolean decend) {
		if (data == null)
			ite = null;
		else{
			if(decend)
				ite = data.descendingIterator();
			else {
				ite = data.iterator();
			}
		}
	}

	Iterator<HeapEntry<T>> ite;

	@Override
	public boolean hasNext() {
		if (ite != null)
			return ite.hasNext();
		else {
			return false;
		}
	}

	@Override
	public HeapEntry<T> next() {
		if (ite != null)
			return ite.next();
		else {
			return null;
		}
	}

	@Override
	public void remove() {
		ite.remove();
	}

}