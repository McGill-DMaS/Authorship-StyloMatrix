/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection.heap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import ca.mcgill.sis.dmas.env.StringResources;

public class Ranker<T> implements Iterable<HeapEntry<T>> {

	public TreeSet<HeapEntry<T>> data = new TreeSet<>();

	public Stream<HeapEntry<T>> stream() {
		return data.stream();
	}

	public HashSet<T> getKeys() {
		return data.stream().map(ent -> ent.value).collect(Collectors.toCollection(HashSet::new));
	}

	public int Capacity = Integer.MAX_VALUE;

	public Ranker() {

	}

	public NavigableSet<HeapEntry<T>> subSet(double start, double end) {
		return data.subSet(new HeapEntry<T>(null, start), true, new HeapEntry<T>(null, end), true);
	}

	public Ranker(int capacity) {
		Capacity = capacity;
		if (this.Capacity < 0)
			this.Capacity = Integer.MAX_VALUE;
	}

	public void clear() {
		data.clear();
	}

	public HeapEntry<T> peekFirst() {
		HeapEntry<T> firstEntry = data.first();
		return firstEntry;
	}

	public HeapEntry<T> peekLast() {
		HeapEntry<T> lastEntry = data.last();
		return lastEntry;
	}

	public HeapEntry<T> pollFirst() {
		HeapEntry<T> firstEntry = data.pollFirst();
		return firstEntry;
	}

	public HeapEntry<T> pollLast() {
		HeapEntry<T> lastEntry = data.pollLast();
		return lastEntry;
	}

	public void push(double score, T value) {
		data.add(new HeapEntry<T>(value, score));
		if (data.size() > Capacity)
			data.pollFirst();
	}

	public void push(Map<T, Double> scores) {
		scores.entrySet().forEach(ent -> this.push(ent.getValue(), ent.getKey()));
	}

	public int size() {
		return data.size();
	}

	@Override
	public Iterator<HeapEntry<T>> iterator() {
		Iterator<HeapEntry<T>> iterator = new HeapIterator<>(data, true);
		return iterator;
	}

	public ArrayList<T> sortedList(boolean ascend) {

		ArrayList<T> list = new ArrayList<T>();

		for (HeapEntry<T> het : this) {
			list.add(het.value);
		}
		if (ascend) {
			list = new ArrayList<>(Lists.reverse(list));
		}
		return list;
	}

}
