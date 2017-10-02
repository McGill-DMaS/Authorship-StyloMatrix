/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection.list;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class MergableLinkedList<T extends Mergable> implements List<T> {

	private int size = 0;
	private Node start = new Node();
	private Node end = new Node();

	private class Node {
		T value;
		Node next = null;
		Node previous = null;
	}

	public MergableLinkedList() {
		start.next = end;
		end.previous = start;
	}

	@Override
	public boolean add(T e) {
		Node node = new Node();
		node.value = e;
		node.next = end;
		node.previous = end.previous;
		end.previous.next = node;
		end.previous = node;
		size++;
		return true;
	}

	@Override
	public void add(int index, T element) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (c == index) {
				Node node = new Node();
				node.value = element;
				node.previous = pointer.previous;
				node.next = pointer;
				pointer.previous.next = node;
				pointer.previous = node;
				size++;
				return;
			}
			c++;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			add(t);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		start.next = end;
		end.previous = start;
		size = 0;
	}

	@Override
	public boolean contains(Object o) {
		Node pointer = start;
		while (pointer.next != end) {
			pointer = pointer.next;
			return o.equals(pointer.value);
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object object : c) {
			if (!contains(object))
				return false;
		}
		return true;
	}

	@Override
	public T get(int index) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (c == index)
				return pointer.value;
			c++;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public int indexOf(Object o) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (o.equals(pointer.value))
				return c;
			c++;
		}
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return start.next == null;
	}

	@Override
	public Iterator<T> iterator() {
		return new MIterator();
	}

	private class MIterator implements Iterator<T> {

		Node pointer = start;

		@Override
		public boolean hasNext() {
			return pointer.next != end;
		}

		@Override
		public T next() {
			pointer = pointer.next;
			return pointer.value;
		}

		@Override
		public void remove() {
			pointer.previous.next = pointer.next;
			pointer.next.previous = pointer.previous;
			pointer = null;
		}

	}

	@Override
	public int lastIndexOf(Object o) {
		Node pointer = end;
		int c = size - 1;
		while (pointer.previous != null) {
			pointer = pointer.previous;
			if (o.equals(pointer.value))
				return c;
			c--;
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
		Node pointer = start;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (o.equals(pointer.value)) {
				pointer.previous.next = pointer.next;
				pointer.next.previous = pointer.previous;
				size--;
			}
		}
		return false;
	}

	@Override
	public T remove(int index) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (c == index) {
				pointer.previous.next = pointer.next;
				pointer.next.previous = pointer.previous;
				size--;
				return pointer.value;
			}
			c++;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object object : c) {
			remove(object);
		}
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Node pointer = start;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (!c.contains(pointer.value)) {
				pointer.previous.next = pointer.next;
				pointer.next.previous = pointer.previous;
				pointer = pointer.previous;
				size--;
			}
		}
		return false;
	}

	@Override
	public T set(int index, T element) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (c == index) {
				pointer.value = element;
			}
			c++;
		}
		throw new IndexOutOfBoundsException();
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
		Object[] array = new Object[size];
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			array[c] = pointer.value;
			c++;
		}
		return array;
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <T> T[] toArray(T[] a) {
		Node pointer = start;
		int c = 0;
		while (pointer.next != end) {
			pointer = pointer.next;
			a[c] = (T) pointer.value;
			c++;
		}
		return a;
	}

	public ArrayList<T> MergeInSequence() {
		ArrayList<T> result = new ArrayList<>();
		Node pointer = start;
		boolean added = false;
		while (pointer.next != end) {
			pointer = pointer.next;
			if (pointer.next != end) {
				if (pointer.value.merge(pointer.next.value)) {
					size--;
					pointer.next.next.previous = pointer;
					pointer.next = pointer.next.next;
					if (!added) {
						result.add(pointer.value);
						added = true;
					}
					pointer = pointer.previous;
				} else {
					added = false;
				}
			}
		}
		return result;
	}

}
