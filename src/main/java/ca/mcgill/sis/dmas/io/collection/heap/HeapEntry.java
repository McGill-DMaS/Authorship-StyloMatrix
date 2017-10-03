/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection.heap;

public class HeapEntry<K> implements Comparable<HeapEntry<K>> {
	public K value;
	public double score;

	public HeapEntry(K value, double score) {
		this.value = value;
		this.score = score;
	}

	@Override
	public int compareTo(HeapEntry<K> o) {
		if (this.score > o.score) {
			return 1;
		} else {
			return -1;
		}
	}
	
	public String toString(){
		return Double.toString(score) + " " + value.toString();
	}

}