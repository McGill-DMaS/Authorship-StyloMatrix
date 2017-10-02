package ca.mcgill.sis.dmas.nlp.model.astyle._5_LDA;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle.Param;

public class LDA {

	public static class LDA_Param extends Param {
		private static final long serialVersionUID = -7202810840534415458L;
		public int iteration = 20;
		public int dimension = 100;
	}

	public LDA_Param param;
	private Model trnModel;
	private Random rand = new Random(1);

	public LDA(LDA_Param param) throws IOException {
		this.param = param;
	}

	public void train(Iterable<Document> docs) {
		Estimator estimator = new Estimator(docs, param.iteration, param.dimension, rand);
		estimator.estimate();
		this.trnModel = estimator.trnModel;
	}

	public Map<String, double[]> getDocEmbedding() {
		return this.trnModel.loadRepresentation();
	}

	public Map<String, double[]> infer(Iterable<Document> docs) {
		Inferencer inferencer = new Inferencer(this.trnModel, rand);
		return inferencer.inferNewDocs(docs);
	}

}
