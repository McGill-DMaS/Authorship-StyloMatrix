package ca.mcgill.sis.dmas.nlp.model.astyle._3_syntactic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.io.collection.IteratorSafeGen;
import ca.mcgill.sis.dmas.io.collection.Pool;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.GradientProgress;
import ca.mcgill.sis.dmas.nlp.model.astyle.NodeWord;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;
import ca.mcgill.sis.dmas.nlp.model.astyle.RandL;
import ca.mcgill.sis.dmas.nlp.model.astyle.WordEmbedding;

import static ca.mcgill.sis.dmas.nlp.model.astyle.MathUtilities.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;

import static java.lang.Math.sqrt;

public class LearnerSyn2VecEmbedding {

	private static Logger logger = LoggerFactory.getLogger(LearnerSyn2VecEmbedding.class);

	public static class S2VParam extends Param {
		private static final long serialVersionUID = -817341338942724187L;
		public int min_freq = 3;
		public int vec_dim = 200;
		public double optm_subsampling = 1e-4;
		public double optm_initAlpha = 0.05;
		public int optm_negSample = 25;
		public int optm_parallelism = 1;
		public int optm_iteration = 20;
		public int optm_aphaUpdateInterval = 10000;

	}

	public static class SEmbedding {
		public Map<String, double[]> synEmbedding;
	}

}
