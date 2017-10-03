/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.arff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.Saver;
import ca.mcgill.sis.dmas.env.DmasApplication;

public class ArffVectorLabelFile {

	private static Logger logger = LoggerFactory.getLogger(ArffVectorLabelFile.class);

	ArffSaver saver;
	int vectorDim;
	ArrayList<String> lbs;
	Instances dataset;
	public String path;

	public ArffVectorLabelFile(String file, int vectorDim, Set<String> labels) throws Exception {
		this(file, "data", vectorDim, labels);
	}

	public ArffVectorLabelFile(String file, String relationName, int vectorDim, Set<String> labels) throws Exception {
		this.path = DmasApplication.applyDataContext(file);
		this.vectorDim = vectorDim;
		ArrayList<weka.core.Attribute> atts = new ArrayList<>();
		for (int i = 0; i < vectorDim; ++i) {
			atts.add(new weka.core.Attribute("att" + i));
		}
		lbs = new ArrayList<>();
		for (String label : labels) {
			lbs.add(label);
		}
		weka.core.Attribute labelAttribute = new weka.core.Attribute("label", lbs);
		atts.add(labelAttribute);
		Instances ins = new Instances("data", atts, 0);
		dataset = ins;

		saver = new ArffSaver();
		saver.setStructure(ins);
		saver.setRetrieval(Saver.INCREMENTAL);
		saver.setFile(new File(DmasApplication.applyDataContext(file)));
	}

	private static double[] extendArrayByOne(double[] array, double val) {
		double[] n_array = new double[array.length + 1];
		System.arraycopy(array, 0, n_array, 0, array.length);
		n_array[n_array.length - 1] = val;
		return n_array;
	}

	public void push(double[] vector, String label) {

		if (vector == null)
			vector = new double[vectorDim];

		try {
			if (vector.length != vectorDim) {
				logger.error("unmatch dimention");
				return;
			}

			// Instance in = new DenseInstance(vectorDim + 1);
			int ind = dataset.attribute(dataset.numAttributes() - 1).indexOfValue(label);
			double[] e_vector = extendArrayByOne(vector, ind);
			DenseInstance in = new DenseInstance(1, e_vector);
			in.setDataset(dataset);

			// for (int j = 0; j < vectorDim; ++j) {
			// in.setValue(j, vector[j]);
			// }
			in.setValue(vectorDim, label);
			synchronized (saver) {
				saver.writeIncremental(in);
			}

		} catch (Exception e) {
			logger.error("Failed to persist instance", e);
		}

	}

	public void close() {
		try {
			saver.writeIncremental(null);
		} catch (IOException e) {
			logger.error("Failed to close the stream.", e);
		}
	}

}
