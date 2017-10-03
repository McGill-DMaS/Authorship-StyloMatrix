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
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java_cup.version;

import com.google.common.collect.ImmutableSet;

import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.Saver;
import ca.mcgill.sis.dmas.env.DmasApplication;

public class ArffVectorFile {
	
	private static Logger logger = LoggerFactory.getLogger(ArffVectorFile.class);

	ArffSaver saver;
	int vectorDim;

	public ArffVectorFile(String file, int vectorDim)
			throws Exception {
		this.vectorDim = vectorDim;
		ArrayList<weka.core.Attribute> atts = new ArrayList<>();
		for (int i = 0; i < vectorDim; ++i) {
			atts.add(new weka.core.Attribute("att" + i));
		}
		Instances ins = new Instances("data", atts, 0);

		saver = new ArffSaver();
		saver.setStructure(ins);
		saver.setRetrieval(Saver.INCREMENTAL);
		saver.setFile(new File(DmasApplication.applyDataContext(file)));
	}

	public void push(double[] vector) {
		
		try {
			if(vector.length != vectorDim){
				logger.error("unmatch dimention");
				return;
			}
				
			Instance in = new DenseInstance(vectorDim);
			for (int j = 0; j < vectorDim; ++j) {
				in.setValue(j, vector[j]);
			}
			
		} catch (Exception e) {
			logger.error("Failed to persist instance");
		}
		
	}
	
	public void close(){
		try {
			saver.writeIncremental(null);
		} catch (IOException e) {
			logger.error("Failed to close the stream.", e);
		}
	}

}
