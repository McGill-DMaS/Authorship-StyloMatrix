/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import ca.mcgill.sis.dmas.env.StringResources;

import com.google.common.collect.Iterables;

/**
 * not a thread-safe implementation
 * @author steven
 *
 * @param <T>
 */
public class MergedList<T> implements List<T> {

	private ArrayList<List<T>> lists;
	private int size = 0;

	@SafeVarargs
	public MergedList(List<T>... lists) {
		this.lists = new ArrayList<>(Arrays.asList(lists));
		for (List<T> list : lists) {
			size += list.size();
		}
	}
	
	@SafeVarargs
	public MergedList(boolean skipEmpty, List<T>... lists) {
		this.lists = new ArrayList<>();
		for (List<T> list : lists) {
			if(skipEmpty && list.size() == 0)
				continue;
			this.lists.add(list);
			size += list.size();
		}
	}
	
	public MergedList(boolean skipEmpty, Iterable<List<T>> lists) {
		this.lists = new ArrayList<>();
		for (List<T> list : lists) {
			if(skipEmpty && list.size() == 0)
				continue;
			this.lists.add(list);
			size += list.size();
		}
	}
	
	public MergedList(Iterable<List<T>> lists) {
		this(true, lists);
	}
	
	public void Add(List<T> list){
		lists.add(list);
		size += list.size();
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		for (List<T> list : lists) {
			if (list.contains(o))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (List<T> list : lists) {
			if (list.containsAll(c))
				return true;
		}
		return false;
	}

	@Override
	public T get(int index) {
		if (index < 0 || index >= size)
			throw new IllegalArgumentException("index:" + index + " size:"
					+ size);
		for (List<T> list : lists) {
			if (index >= list.size())
				index -= list.size();
			else
				return list.get(index);
		}
		throw new IllegalArgumentException("index:" + index + " size:" + size);
	}

	@Override
	public int indexOf(Object o) {
		int index = 0;
		for (List<T> list : lists) {
			int sub_index = list.indexOf(o);
			if (sub_index == -1) {
				index += list.size();
			} else {
				return index + sub_index;
			}
		}
		return -1;
	}

	@Override
	public boolean isEmpty() {
		for (List<T> list : lists) {
			if (!list.isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public Iterator<T> iterator() {
		return Iterables.concat(lists).iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		int index = size - 1;
		for (int i = lists.size() - 1; i >= 0; --i) {
			List<T> list = lists.get(i);
			index = index - list.size() + 1;
			int sub_index = list.lastIndexOf(o);
			if (sub_index == -1) {
				continue;
			} else {
				return index + sub_index;
			}
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}
	
	public String toString(){
		return StringResources.JOINER_TOKEN.join(lists);
	}

}
