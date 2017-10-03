package ca.mcgill.sis.dmas.nlp.model.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class NeuronWord extends Neuron {
    public String name;
    public double[] syn0 = null; 
    public Neuron[] neurons = null;
    public int[] codeArr = null;

    public Neuron[] makeNeurons() {
        if (neurons != null) {
            return neurons;
        }
        Neuron neuron = this;
        ArrayList<Neuron> neuronsList = new ArrayList<>();
        while ((neuron = neuron.parent) != null) {
        	neuronsList.add(neuron);
        }
        Collections.reverse(neuronsList);
        codeArr = new int[neuronsList.size()];

        for (int i = 1; i < neuronsList.size(); i++) {
            codeArr[i - 1] = neuronsList.get(i).code;
        }
        codeArr[codeArr.length - 1] = this.code;
        neurons = neuronsList.toArray(new Neuron[neuronsList.size()]);

        return neurons;
    }
    
    
    public static Random random = new Random(System.currentTimeMillis());

    public NeuronWord(String name, int freq, int layerSize) {
        this.name = name;
        this.freq = freq;
        this.syn0 = new double[layerSize];
        for (int i = 0; i < syn0.length; i++) {
            syn0[i] = (float) ((random.nextFloat() - 0.5) / layerSize);
        }
    }

}