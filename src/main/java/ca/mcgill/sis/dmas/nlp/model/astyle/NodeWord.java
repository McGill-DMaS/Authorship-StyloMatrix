package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.io.Serializable;
import java.util.Random;

import ca.mcgill.sis.dmas.env.StringResources;

public class NodeWord implements Serializable {

	private static final long serialVersionUID = 1982433642098461988L;

	public String token = null;
	public double[] neuIn = null;
	public double[] neuOut = null;
	public boolean fixed = false;

	public long freq = 0;
	public double samProb = 1.0;

	public NodeWord(String token, long freq) {
		this.token = token;
		this.freq = freq;
	}

	public void init(int dim, RandL rl) {
		this.initInLayer(dim, rl);
		this.initOutLayer(dim);
	}

	public void initOutLayer(int dim) {
		this.neuOut = new double[dim];
		for (int j = 0; j < dim; ++j)
			neuOut[j] = 0;
	}

	public void initInLayer(int dim, RandL rl) {
		this.neuIn = new double[dim];
		for (int i = 0; i < dim; ++i)
			neuIn[i] = (rl.nextF() - 0.5) / dim;
	}

	@Override
	public String toString() {
		return token;// + "::" + freq + "::" +
						// StringResources.FORMAT_2R2D.format(samProb);
	}
}
