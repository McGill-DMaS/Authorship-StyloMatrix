package ca.mcgill.sis.dmas.nlp.model.astyle._6_LSA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.io.collection.heap.Ranker;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;

public class LatentSemanticIndex {

	private static Logger logger = LoggerFactory.getLogger(LatentSemanticIndex.class);

	public boolean progress = false;
	private int topk;

	public static class LSI_Param extends Param {
		public int dimension = 100;
		public int topkToken = 8000;
	}

	public LatentSemanticIndex(LSI_Param param) {
		this.dimension = param.dimension;
		this.topk = param.topkToken;
	}

	public int dimension = 100;
	private RealMatrix u_sInvered;

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

		if (progress)
			logger.info("Constructing matrix input.");
		map = new WordIndexMap(ranker.getKeys());
		docs.forEach(doc -> map.countWordInDoc(doc, true));
		int numTkns = map.dim();

		OpenMapRealMatrix word2doc = new OpenMapRealMatrix(numTkns, numDocs);

		docs.forEach(doc -> {
			HashMap<Integer, Double> vec = map.countWordInDoc(doc, false);
			Integer ind = docIndMap.get(doc.id);
			vec.entrySet().stream().forEach(ent -> {
				word2doc.setEntry(ent.getKey(), ind, ent.getValue());
			});
		});

		if (progress)
			logger.info("Decomposing matrix...");
		SingularValueDecomposition fullSVD = new SingularValueDecomposition(word2doc);
		RealMatrix[] factorization = reduceFullSVD(fullSVD, dimension);
		RealMatrix U = factorization[0];
		RealMatrix S = factorization[1];
		RealMatrix V = factorization[2];
		logger.info("generating representations.");

		// document -> latent representation
		double[][] docRep = S.multiply(V).transpose().getData();
		docRepMap = IntStream.range(0, docRep.length).mapToObj(ind -> new Integer(ind))
				.collect(Collectors.toMap(ind -> indDocMap.get(ind), ind -> docRep[ind]));

		// for projection:
		// invert S:
		if (progress)
			logger.info("Generating training model...");
		for (int i = 0; i < S.getRowDimension(); i++)
			S.setEntry(i, i, 1.0 / S.getEntry(i, i));
		this.u_sInvered = U.multiply(S);
	}

	public Map<String, double[]> getDocEmbedding() {
		return this.docRepMap;
	}

	public Map<String, double[]> inferDocs(Iterable<Document> docs) {

		logger.info("Constructing matrix input (inference).");
		ArrayList<String> indDocMap = StreamSupport.stream(docs.spliterator(), false).map(doc -> doc.id)
				.collect(Collectors.toCollection(ArrayList::new));
		int numDocs = indDocMap.size();
		Map<String, Integer> docIndMap = IntStream.range(0, indDocMap.size()).mapToObj(ind -> new Integer(ind))
				.collect(Collectors.toMap(ind -> indDocMap.get(ind), ind -> ind));

		int numTkns = map.dim();
		OpenMapRealMatrix word2doc = new OpenMapRealMatrix(numDocs, numTkns);
		docs.forEach(doc -> {
			HashMap<Integer, Double> vec = map.countWordInDoc(doc, false);
			Integer ind = docIndMap.get(doc.id);
			vec.entrySet().stream().forEach(ent -> {
				word2doc.setEntry(ind, ent.getKey(), ent.getValue());
			});
		});
		logger.info("Infering...");
		// (|docs|, |tkns|) * (|tkns|, dim) * (dim, dim)
		double[][] docRep = word2doc.multiply(u_sInvered).getData();
		return IntStream.range(0, docRep.length).mapToObj(ind -> new Integer(ind))
				.collect(Collectors.toMap(ind -> indDocMap.get(ind), ind -> docRep[ind]));

	}

	public static RealMatrix[] reduceFullSVD(SingularValueDecomposition fullSVD, int dim) {
		RealMatrix u = fullSVD.getU();
		RealMatrix s = fullSVD.getS();
		RealMatrix v = fullSVD.getVT();

		int actualEndInd = dim > s.getRowDimension() ? (s.getRowDimension() - 1) : (dim - 1);
		RealMatrix su = u.getSubMatrix(0, u.getRowDimension() - 1, 0, actualEndInd);
		RealMatrix ss = s.getSubMatrix(0, actualEndInd, 0, actualEndInd);
		RealMatrix sv = v.getSubMatrix(0, actualEndInd, 0, v.getColumnDimension() - 1);
		return new RealMatrix[] { su, ss, sv };
	}

	public static void main(String[] args) {

		double[][] A = new double[][] { //
				{ 4, 1, 2, 3, 4 }, //
				{ 3, 6, 3, 4, 5 }, //
				{ 6, 7, 7, 8, 9 }, //
				{ 6, 7, 4, 8, 9 }, //
				{ 6, 7, 2, 9, 9 } };

		RealMatrix mA = MatrixUtils.createRealMatrix(A);
		RealMatrix[] ms = reduceFullSVD(new SingularValueDecomposition(mA), 3);
		printInfo(ms[0].multiply(ms[1]).multiply(ms[2]));

	}

	public static void printInfo(RealMatrix mat) {
		System.out.println(mat.getRowDimension() + "\t" + mat.getColumnDimension());
		System.out.println(mat);
	}

}
