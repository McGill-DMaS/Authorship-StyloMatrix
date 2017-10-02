package ca.mcgill.sis.dmas.nlp.model.embedding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class Word {
	public int freq;
	public String word;
	public int[] codeArr = null;
	public double[] vector;
	public int[] parents;

	public Word(Neuron neuron, int vectorSize, Random random) {
		Neuron neuronP = neuron;
		LinkedList<Neuron> neuronsList = new LinkedList<>();
		while ((neuronP = neuronP.parent) != null) {
			neuronsList.add(neuronP);
		}
		Collections.reverse(neuronsList);
		codeArr = new int[neuronsList.size()];
		parents = new int[neuronsList.size()];

		for (int i = 0; i < neuronsList.size() - 1; i++) {
			Neuron neuronI = neuronsList.get(i);
			Neuron neuronIAfter = neuronsList.get(i + 1);
			codeArr[i] = neuronIAfter.code;
			parents[i] = ((NeuronHidden) neuronI).index;
		}
		codeArr[codeArr.length - 1] = neuron.code;
		parents[parents.length - 1] = ((NeuronHidden) neuronsList.getLast()).index;

		vector = new double[vectorSize];
		for (int i = 0; i < vectorSize; ++i) {
			vector[i] = (random.nextDouble() - 0.5) / vectorSize;
		}
		freq = neuron.freq;
		word = ((NeuronWord) neuron).name;
	}

	public Word() {
	}

	public Word(int vectorSize, Random random) {
		vector = new double[vectorSize];
		for (int i = 0; i < vectorSize; ++i) {
			vector[i] = (random.nextDouble() - 0.5) / vectorSize;
		}
	}

	public void write(DataOutputStream dataOutputStream) throws Exception {
		dataOutputStream.writeInt(freq);
		dataOutputStream.writeUTF(word);
		dataOutputStream.writeInt(codeArr.length);
		for (int i = 0; i < codeArr.length; ++i)
			dataOutputStream.writeInt(codeArr[i]);
		for (int i = 0; i < vector.length; ++i)
			dataOutputStream.writeDouble(vector[i]);
		for (int i = 0; i < parents.length; ++i)
			dataOutputStream.writeInt(parents[i]);

	}

	public void read(DataInputStream dataInputStream, int vectorSize)
			throws Exception {
		freq = dataInputStream.readInt();
		word = dataInputStream.readUTF();
		int codeArrLength = dataInputStream.readInt();
		codeArr = new int[codeArrLength];
		for (int i = 0; i < codeArrLength; ++i)
			codeArr[i] = dataInputStream.readInt();
		vector = new double[vectorSize];
		for (int i = 0; i < vectorSize; ++i)
			vector[i] = dataInputStream.readDouble();
		parents = new int[codeArrLength];
		for (int i = 0; i < codeArrLength; ++i)
			parents[i] = dataInputStream.readInt();
	}

	public String toString() {
		return word;
	}

	public static class SoftMaxWord extends Word {
		public double[] outVector;
		
		public SoftMaxWord(int vectorSize, Random random){
			super(vectorSize, random);
			outVector = new double[vectorSize];
			for (int i = 0; i < vectorSize; ++i) {
				outVector[i] = (random.nextDouble() - 0.5) / vectorSize;
			}
		}

		@Override
		public void write(DataOutputStream dataOutputStream) throws Exception {
			dataOutputStream.writeInt(freq);
			dataOutputStream.writeUTF(word);
			for (int i = 0; i < vector.length; ++i)
				dataOutputStream.writeDouble(vector[i]);
			for (int i = 0; i < outVector.length; ++i)
				dataOutputStream.writeDouble(outVector[i]);
		}

		@Override
		public void read(DataInputStream dataInputStream, int vectorSize)
				throws Exception {
			freq = dataInputStream.readInt();
			word = dataInputStream.readUTF();
			vector = new double[vectorSize];
			for (int i = 0; i < vectorSize; ++i)
				vector[i] = dataInputStream.readDouble();
			outVector = new double[vectorSize];
			for (int i = 0; i < vectorSize; ++i)
				outVector[i] = dataInputStream.readDouble();
		}
	}
}