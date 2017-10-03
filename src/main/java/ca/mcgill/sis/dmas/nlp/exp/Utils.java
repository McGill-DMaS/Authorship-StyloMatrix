package ca.mcgill.sis.dmas.nlp.exp;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.heap.Ranker;

public class Utils {

	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	public static class TestEntries<T> {
		public HashMap<String, TestEntry<T>> cache = new HashMap<>();
		public T fin = null;
		public String file = StringResources.STR_EMPTY;

		private static ObjectMapper mapper = new ObjectMapper();

		public static <T> void update(String cacheFile, Class<T> cls, TestEntry<T> entry) {
			try {
				synchronized (mapper) {
					TestEntries<T> entries = new TestEntries<T>();
					if (new File(cacheFile).exists())
						entries = read(mapper, cacheFile, cls);
					entries.cache.put(entry.param.toString(), entry);
					mapper.writerWithDefaultPrettyPrinter().writeValue(new File(cacheFile), entries);
				}
			} catch (Exception e) {
				logger.error("Failed to update cache results", e);
			}

		}

		public static <T> TestEntry<T> retrieve(String cacheFile, Class<T> cls, T param) {
			try {
				synchronized (mapper) {
					TestEntries<T> entries = new TestEntries<T>();
					if (new File(cacheFile).exists())
						entries = read(mapper, cacheFile, cls);
					return entries.cache.get(param.toString());
				}
			} catch (Exception e) {
				logger.error("Failed to update cache results", e);
			}
			return null;
		}

		public static <T> T currentMax(String cacheFile, Class<T> cls) {
			try {
				synchronized (mapper) {
					TestEntries<T> entries = null;
					if (new File(cacheFile).exists())
						entries = read(mapper, cacheFile, cls);
					if (entries == null || entries.fin == null)
						return null;
					return entries.fin;
				}
			} catch (Exception e) {
				logger.error("Failed to update cache results", e);
			}
			return null;
		}
	}

	public static <T> TestEntries<T> read(ObjectMapper mapper, String file, Class<T> contentClass) throws Exception {
		JavaType type = mapper.getTypeFactory().constructParametrizedType(TestEntries.class, TestEntries.class,
				contentClass);
		return mapper.readValue(new File(file), type);
	}

	public static class TestEntry<T> {
		public double score = 0;
		public T param;

		public TestEntry(double score, T param) {
			this.score = score;
			this.param = param;
		}

		public TestEntry() {
		}

		@Override
		public String toString() {
			return StringResources.FORMAT_AR3D.format(score) + " " + param.toString();
		}
	}

	public static <T> List<TestEntry<T>> tune(Function<T, TestEntry<T>> tester, Supplier<T> defaultSupplier,
			List<EntryPair<String, Object[]>> vals, int samples) {
		List<T> params = setField(defaultSupplier, vals, samples);
		Ranker<TestEntry<T>> ranks = new Ranker<>(samples);
		params.parallelStream().forEach(param -> {
			logger.info("Running for {}", param.toString());
			try {
				TestEntry<T> res = tester.apply(param);
				synchronized (ranks) {
					ranks.push(res.score, res);
					System.out.println("Added new entry. Current rank: total(" + ranks.size() + ")");
					ranks.data.forEach(System.out::println);
				}
			} catch (Exception e) {
				logger.error("Error in testing.", e);
			}
		});
		return ranks.sortedList(false);
	}

	public static <T> List<T> setField(Supplier<T> defaultSupplier, List<EntryPair<String, Object[]>> vals,
			int samples) {
		HashMap<String, Object> init = new HashMap<>();
		List<HashMap<String, Object>> res = combine(init, vals, 0);
		Collections.shuffle(res, new Random(0));
		logger.info("Total {} params. Pick {}.", res.size(), samples);
		if (samples != -1 && samples < res.size()) {
			res = res.subList(0, samples);
		}
		return res.stream().map(fields -> {
			T param = defaultSupplier.get();
			fields.forEach((name, val) -> {
				try {
					Field field = param.getClass().getDeclaredField(name);
					field.set(param, val);
				} catch (Exception e) {
					logger.error("Failed to set field " + name + " of val " + val, e);
				}
			});
			return param;
		}).collect(Collectors.toList());
	}

	private static List<HashMap<String, Object>> combine(HashMap<String, Object> lastState,
			List<EntryPair<String, Object[]>> vals, int ind) {
		ArrayList<HashMap<String, Object>> results = new ArrayList<>();
		if (vals.size() < 1) {
			return results;
		}
		if (ind == vals.size())
			return Arrays.asList(lastState);
		String fieldName = vals.get(ind).key;
		Object[] values = vals.get(ind).value;
		for (Object val : values) {
			HashMap<String, Object> entry = new HashMap<>(lastState);
			entry.put(fieldName, val);
			List<HashMap<String, Object>> nextLevels = combine(entry, vals, ind + 1);
			results.addAll(nextLevels);
		}
		return results;
	}
}
