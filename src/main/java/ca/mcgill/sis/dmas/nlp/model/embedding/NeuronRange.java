package ca.mcgill.sis.dmas.nlp.model.embedding;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NeuronRange implements Serializable {

	private static final long serialVersionUID = 8472553287744897985L;
	
	
	public int id;
	public double[] syn0 = null;

	public NeuronRange() {
	}

	public NeuronRange(int id, int vectorSize, Random random) {
		this.id = id;

		this.syn0 = new double[vectorSize];
		for (int i = 0; i < syn0.length; i++) {
			syn0[i] = ((random.nextDouble() - 0.5) / vectorSize);
		}
	}

	public void normalize() {
		double len = 0;
		for (int i = 0; i < syn0.length; ++i) {
			len += syn0[i] * syn0[i];
		}

		len = Math.sqrt(len);

		for (int i = 0; i < syn0.length; ++i) {
			syn0[i] /= len;
		}
	}

	public NeuronRange(int vectorSize, boolean allZero, Random random) {
		this.syn0 = new double[vectorSize];

		if (allZero) {
			Arrays.fill(syn0, 0);
		} else {

			for (int i = 0; i < syn0.length; i++) {
				syn0[i] = ((random.nextDouble() - 0.5) / vectorSize);
			}
		}
	}
}
