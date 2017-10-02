package ca.mcgill.sis.dmas.nlp.model.astyle._6_LSA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
// $example on$
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Matrices;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.SingularValueDecomposition;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.linalg.distributed.RowMatrix;

import ca.mcgill.sis.dmas.io.collection.heap.Ranker;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;
import ca.mcgill.sis.dmas.nlp.model.astyle._6_LSA.LatentSemanticIndex.LSI_Param;

public class LatentSemanticIndexSpark {

	private static Logger logger = LoggerFactory.getLogger(LatentSemanticIndexSpark.class);

	private static SparkConf conf = new SparkConf().setMaster("local[8]").setAppName("LSI-SVD");
	private static SparkContext sc = new SparkContext(conf);
	private static JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sc);

	public boolean progress = false;
	private int topk;

	public LatentSemanticIndexSpark(LSI_Param param) {
		this.dimension = param.dimension;
		this.topk = param.topkToken;
	}

	public int dimension = 100;

	Map<String, double[]> docRepMap;
	WordIndexMap map;

	public void train(Iterable<Document> docs) {

		ArrayList<String> indDocMap = StreamSupport.stream(docs.spliterator(), false).map(doc -> doc.id)
				.collect(Collectors.toCollection(ArrayList::new));
		int numDocs = indDocMap.size();
		Map<String, Integer> docIndMap = IntStream.range(0, indDocMap.size()).mapToObj(ind -> new Integer(ind))
				.collect(Collectors.toMap(ind -> indDocMap.get(ind), ind -> ind));

		if (progress)
			logger.info("Getting top-k frequent words.");
		HashMap<String, Double> counter = new HashMap<>();
		StreamSupport.stream(docs.spliterator(), false).flatMap(doc -> doc.sentences.stream())
				.flatMap(sent -> Arrays.stream(sent.tokens)).forEach(tkn -> {
					counter.compute(tkn, (k, v) -> v == null ? 1. : v + 1);
				});
		Ranker<String> ranker = new Ranker<>(this.topk);
		ranker.push(counter);
		map = new WordIndexMap(ranker.getKeys());
		docs.forEach(doc -> map.countWordInDoc(doc, true));
		int numTkns = map.dim();

		if (progress)
			logger.info("Constructing matrix input.");

		List<Vector> data = new ArrayList<>();
		Vector zeros = Vectors.zeros(numTkns);
		for (int i = 0; i < indDocMap.size(); ++i)
			data.add(zeros);
		docs.forEach(doc -> {
			HashMap<Integer, Double> vec = map.countWordInDoc(doc, false);
			Integer docInd = docIndMap.get(doc.id);
			int[] indx = vec.entrySet().stream().mapToInt(ent -> ent.getKey()).toArray();
			double[] vals = vec.entrySet().stream().mapToDouble(ent -> ent.getValue()).toArray();
			data.set(docInd, Vectors.sparse(numTkns, indx, vals));
		});

		JavaRDD<Vector> rows = jsc.parallelize(data);
		RowMatrix mat = new RowMatrix(rows.rdd());

		if (progress)
			logger.info("Decomposing matrix...");

		SingularValueDecomposition<RowMatrix, Matrix> svd = mat.computeSVD(this.dimension, true, 1.0E-9d);
		RowMatrix U = svd.U(); // The U factor is a RowMatrix.
		Vector s = svd.s(); // The singular values are stored in a local dense
							// vector.

		logger.info("generating representations.");

		RowMatrix rep = U.multiply(Matrices.diag(s));
		Vector[] docRep = (Vector[]) rep.rows().collect();

		logger.info("{} rows vs {} docs", docRep.length, numDocs);

		// document -> latent representation
		docRepMap = IntStream.range(0, docRep.length).mapToObj(ind -> new Integer(ind))
				.collect(Collectors.toMap(ind -> indDocMap.get(ind), ind -> docRep[ind].toArray()));

	}

	public Map<String, double[]> getDocEmbedding() {
		return this.docRepMap;
	}

	public Map<String, double[]> inferDocs(Iterable<Document> docs) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
