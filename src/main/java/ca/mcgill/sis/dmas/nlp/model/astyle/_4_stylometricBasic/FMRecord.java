/**
 * @author Benjamin Fung
 * The database for features.
 */

package ca.mcgill.sis.dmas.nlp.model.astyle._4_stylometricBasic;

import java.util.ArrayList;

public class FMRecord {

	private ArrayList<Double> vals = new ArrayList<>();

	public boolean add(double value) {
		if (Double.isNaN(value))
			return vals.add(0.0);
		else
			return vals.add(value);
	}

	public boolean add(boolean value) {
		if (value)
			return add(1);
		else
			return add(0);
	}

	public boolean add(int[] values) {
		for (int i = 0; i < values.length; ++i)
			vals.add((double) values[i]);
		return true;
	}

	public boolean add(double[] values) {
		for (int i = 0; i < values.length; ++i)
			vals.add(values[i]);
		return true;
	}

	public boolean add(boolean[] values) {
		for (int i = 0; i < values.length; ++i)
			add(values[i]);
		return true;
	}

	public double[] toVec() {
		return vals.stream().mapToDouble(d -> {
			if(Double.isInfinite(d) || Double.isNaN(d))
				return 0;
			else
				return d;
		}).toArray();
	}

}
