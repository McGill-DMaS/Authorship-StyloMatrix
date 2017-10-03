/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class DmasCollectionOperations {

	private static Logger logger = LoggerFactory.getLogger(DmasCollectionOperations.class);

	public static List<String> combinationDuplicated(Set<String> candiates, int size) {
		return combinationDuplicated(candiates, new ArrayList<>(candiates), size - 1);
	}

	private static List<String> combinationDuplicated(Set<String> candiates, List<String> state, int size) {
		if (size == 0) {
			return state;
		} else {
			state = state.stream().flatMap(str -> candiates.stream().map(cand -> {
				return str + "," + cand;
			}).collect(Collectors.toList()).stream()).collect(Collectors.toList());
			return combinationDuplicated(candiates, state, size - 1);
		}

	}
	

	public static <T> Set<T> depulicatedInterset(List<Set<T>> sets) {

		if (sets.size() < 1)
			return null;
		if (sets.size() == 1)
			return sets.get(0);
		Set<T> result = new HashSet<>();

		for (T value : sets.get(0)) {
			boolean add = true;
			for (int i = 1; i < sets.size(); ++i) {
				if (!sets.get(i).contains(value)) {
					add = false;
					break;
				}
			}
			if (add)
				result.add(value);
		}
		return result;
	}

	public static long count(Iterable<?> ite) {
		long res = 0;
		for (@SuppressWarnings("unused")
		Object string : ite) {
			res++;
		}
		return res;
	}

	public static <T> ArrayList<Iterable<T>> split(Iterable<T> iterable, int numberOfSplits) {
		ArrayList<Iterable<T>> result = new ArrayList<Iterable<T>>(numberOfSplits);
		long size = count(iterable);
		int foldSize = (int) (size / numberOfSplits);
		int residue = (int) (size % numberOfSplits);
		if (residue != 0)
			foldSize++;
		for (int i = 0; i < numberOfSplits; i++) {
			int t_start = i * foldSize;

			result.add(Iterables.limit(Iterables.skip(iterable, t_start), foldSize));

			if (residue != 0) {
				residue--;
				if (residue == 0) {
					foldSize--;
				}
			}
		}
		return result;
	}

	public static <T> ArrayList<Iterable<T>> split(Iterable<T> iterable, int numberOfSplits, int numOfRepeat) {
		ArrayList<Iterable<T>> result = new ArrayList<Iterable<T>>(numberOfSplits);
		long size = count(iterable);
		int foldSize = (int) (size / numberOfSplits);
		int residue = (int) (size % numberOfSplits);
		if (residue != 0)
			foldSize++;
		for (int i = 0; i < numberOfSplits; i++) {
			int t_start = i * foldSize;

			Iterable<T> range = Iterables.limit(Iterables.skip(iterable, t_start), foldSize);
			ArrayList<Iterable<T>> ranges = new ArrayList<>();
			for (int j = 0; j < numOfRepeat; j++)
				ranges.add(range);

			result.add(Iterables.concat(ranges));

			if (residue != 0) {
				residue--;
				if (residue == 0) {
					foldSize--;
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {
		// List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
		// int numIteration = 3;
		// int numOfThread = 3;
		//
		// final ArrayList<Iterable<Integer>> iterables =
		// DmasCollectionOperations.split(list, numOfThread, numIteration);
		//
		// long tsize = 0;
		// for (Iterable<Integer> iterable : iterables) {
		// long size = DmasCollectionOperations.count(iterable);
		// tsize += size;
		// logger.info("iterable size: {}", size);
		// logger.info(iterable.toString());
		// }3
		// logger.info("Total size: {}/{}/{}", tsize,
		// DmasCollectionOperations.count(list) * numIteration);
		
		combinationDuplicated(Sets.newHashSet("A","B", "C"), 3).stream().forEach(System.out::println);
	}

	public static <A, B, C> Stream<C> zip(Stream<? extends A> a, Stream<? extends B> b,
			BiFunction<? super A, ? super B, ? extends C> zipper) {
		Objects.requireNonNull(zipper);
		@SuppressWarnings("unchecked")
		Spliterator<A> aSpliterator = (Spliterator<A>) Objects.requireNonNull(a).spliterator();
		@SuppressWarnings("unchecked")
		Spliterator<B> bSpliterator = (Spliterator<B>) Objects.requireNonNull(b).spliterator();

		// Zipping looses DISTINCT and SORTED characteristics
		int both = aSpliterator.characteristics() & bSpliterator.characteristics()
				& ~(Spliterator.DISTINCT | Spliterator.SORTED);
		int characteristics = both;

		long zipSize = ((characteristics & Spliterator.SIZED) != 0)
				? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown()) : -1;

		Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
		Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
		Iterator<C> cIterator = new Iterator<C>() {
			@Override
			public boolean hasNext() {
				return aIterator.hasNext() && bIterator.hasNext();
			}

			@Override
			public C next() {
				return zipper.apply(aIterator.next(), bIterator.next());
			}
		};

		Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
		return (a.isParallel() || b.isParallel()) ? StreamSupport.stream(split, true)
				: StreamSupport.stream(split, false);
	}

}
