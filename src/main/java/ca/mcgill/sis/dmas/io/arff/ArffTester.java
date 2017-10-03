/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.arff;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.omg.CORBA.DATA_CONVERSION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.DmasApplication;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SimpleLogistic;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Debug.Random;
import weka.core.OptionHandler;
import weka.core.converters.ConverterUtils.DataSource;

public class ArffTester {

	public volatile Class<?> classifierClass = LibLINEAR.class;
	public volatile Class<?> classifierClassNumeric = LinearRegression.class;
	public volatile String[] options = new String[] {};

	private static Logger logger = LoggerFactory.getLogger(ArffTester.class);

	public ArrayList<Double> CV10(String... arffs) {
		return CV10(Arrays.asList(arffs));
	}

	public ArrayList<Double> CV10(File... arffs) {
		ArrayList<String> fullPaths = new ArrayList<>();
		for (File f : arffs) {
			fullPaths.add(f.getAbsolutePath());
		}
		return CV10(fullPaths);
	}

	public ArrayList<Double> CV10(Iterable<String> arffs) {

		ArrayList<Double> precisions = new ArrayList<>();

		for (String arffVectorLabelFile : arffs) {

			arffVectorLabelFile = DmasApplication.applyDataContext(arffVectorLabelFile);
			try {
				Instances data = DataSource.read(arffVectorLabelFile);
				data.setClassIndex(data.numAttributes() - 1);
				precisions.add(CV10(data, false));
			} catch (Exception e) {
				logger.error("Failed to test arff file: " + arffVectorLabelFile, e);
				continue;
			}

		}

		return precisions;
	}

	public Double CV10(Instances data, boolean printDetail) throws Exception {

		// randomize data
		Random rand = new Random(0);
		Instances randData = new Instances(data);
		randData.randomize(rand);
		if (randData.classAttribute().isNominal())
			randData.stratify(10);

		Evaluation eval = new Evaluation(randData);

		// perform cross-validation
		for (int n = 0; n < 10; n++) {
			Instances train = randData.trainCV(10, n);
			Instances test = randData.testCV(10, n);

			Classifier cls;
			if (data.classAttribute().isNumeric())
				cls = (Classifier) classifierClassNumeric.newInstance();
			else
				cls = (Classifier) classifierClass.newInstance();
			if (cls instanceof OptionHandler && options.length != 0)
				((OptionHandler) cls).setOptions(options);

			cls.buildClassifier(train);
			eval.evaluateModel(cls, test);
		}

		if (printDetail) {
			System.out.println(eval.toSummaryString(true));
			if (!data.classAttribute().isNumeric()) {
				System.out.println(eval.toClassDetailsString());
				System.out.println(eval.toMatrixString());
			}
		}
		double precision = eval.correct() / (eval.correct() + eval.incorrect());
		return precision;
	}

	public Evaluation CV10(String file, boolean printDetail) throws Exception {
		
		Instances data = DataSource.read(file);
		data.setClassIndex(data.numAttributes() - 1);

		// randomize data
		Random rand = new Random(0);
		Instances randData = new Instances(data);
		randData.randomize(rand);
		if (randData.classAttribute().isNominal())
			randData.stratify(10);

		Evaluation eval = new Evaluation(randData);

		// perform cross-validation
		for (int n = 0; n < 10; n++) {
			Instances train = randData.trainCV(10, n);
			Instances test = randData.testCV(10, n);

			Classifier cls;
			if (data.classAttribute().isNumeric())
				cls = (Classifier) classifierClassNumeric.newInstance();
			else
				cls = (Classifier) classifierClass.newInstance();
			if (cls instanceof OptionHandler && options.length != 0)
				((OptionHandler) cls).setOptions(options);

			cls.buildClassifier(train);
			eval.evaluateModel(cls, test);
		}

		if (printDetail) {
			System.out.println(eval.toSummaryString(true));
			if (!data.classAttribute().isNumeric()) {
				System.out.println(eval.toClassDetailsString());
				System.out.println(eval.toMatrixString());
			}
		}
		return eval;
	}
	
	public Double CVleaveOneOut(Instances data, boolean printDetail) throws Exception {

		// randomize data
		Random rand = new Random(0);
		Instances randData = new Instances(data);
		randData.randomize(rand);
		if (randData.classAttribute().isNominal())
			randData.stratify(data.size());

		Evaluation eval = new Evaluation(randData);

		// perform cross-validation
		for (int n = 0; n < data.size(); n++) {
			Instances train = randData.trainCV(data.size(), n);
			Instances test = randData.testCV(data.size(), n);

			Classifier cls;
			if (data.classAttribute().isNumeric())
				cls = (Classifier) classifierClassNumeric.newInstance();
			else
				cls = (Classifier) classifierClass.newInstance();
			if (cls instanceof OptionHandler && options.length != 0)
				((OptionHandler) cls).setOptions(options);

			cls.buildClassifier(train);
			eval.evaluateModel(cls, test);
		}

		if (printDetail) {
			System.out.println(eval.toSummaryString(true));
			if (!data.classAttribute().isNumeric()) {
				System.out.println(eval.toClassDetailsString());
				System.out.println(eval.toMatrixString());
			}
		}
		double precision = eval.correct() / (eval.correct() + eval.incorrect());
		return precision;
	}

	public ArrayList<Evaluation> CVn(Classifier[] cls, int n, String... arffs) {
		return CVn(cls, n, Arrays.asList(arffs));
	}

	public ArrayList<Evaluation> CVn(Classifier[] cls, int nt, Iterable<String> arffs) {

		ArrayList<Evaluation> results = new ArrayList<>();

		for (String arffVectorLabelFile : arffs) {

			arffVectorLabelFile = DmasApplication.applyDataContext(arffVectorLabelFile);

			Instances data;
			try {

				data = DataSource.read(arffVectorLabelFile);
				data.setClassIndex(data.numAttributes() - 1);
				// randomize data
				Random rand = new Random(0);
				Instances randData = new Instances(data);
				randData.randomize(rand);
				if (randData.classAttribute().isNominal())
					randData.stratify(nt);

				Evaluation eval = new Evaluation(randData);

				// perform cross-validation
				for (int n = 0; n < nt; n++) {
					Instances train = randData.trainCV(nt, n);
					Instances test = randData.testCV(nt, n);

					cls[n].buildClassifier(train);
					eval.evaluateModel(cls[n], test);
				}

				results.add(eval);
			} catch (Exception e) {
				logger.error("Failed to test arff file: " + arffVectorLabelFile, e);
				continue;
			}

		}

		return results;
	}

	public double TS(String trainingSets, String testingSet) {

		try {
			Instances trainingdata = DataSource.read(DmasApplication.applyDataContext(trainingSets));
			Instances testingdata = DataSource.read(DmasApplication.applyDataContext(testingSet));

			trainingdata.setClassIndex(trainingdata.numAttributes() - 1);
			testingdata.setClassIndex(testingdata.numAttributes() - 1);

			Evaluation eval = new Evaluation(trainingdata);

			Classifier cls = (Classifier) classifierClass.newInstance();
			if (cls instanceof OptionHandler && options.length != 0)
				((OptionHandler) cls).setOptions(options);

			cls.buildClassifier(trainingdata);
			eval.evaluateModel(cls, testingdata);
			double precision = eval.correct() / (eval.correct() + eval.incorrect());
			return precision;
		} catch (Exception e) {
			logger.error("Failed to test arff file: " + trainingSets + " on " + testingSet, e);
			return 0;
		}

	}

	public Evaluation TS_Eval(String trainingSets, String testingSet) {

		try {
			Instances trainingdata = DataSource.read(DmasApplication.applyDataContext(trainingSets));
			Instances testingdata = DataSource.read(DmasApplication.applyDataContext(testingSet));

			trainingdata.setClassIndex(trainingdata.numAttributes() - 1);
			testingdata.setClassIndex(testingdata.numAttributes() - 1);

			Evaluation eval = new Evaluation(trainingdata);

			Classifier cls = (Classifier) classifierClass.newInstance();
			if (cls instanceof OptionHandler && options.length != 0)
				((OptionHandler) cls).setOptions(options);

			cls.buildClassifier(trainingdata);
			eval.evaluateModel(cls, testingdata);
			return eval;
		} catch (Exception e) {
			logger.error("Failed to test arff file: " + trainingSets + " on " + testingSet, e);
			return null;
		}

	}

}
