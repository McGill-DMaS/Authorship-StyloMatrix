package ca.mcgill.sis.dmas.nlp.model.embedding;

import java.util.Collection;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Haffman {
	
	Logger logger = LoggerFactory.getLogger(Haffman.class);
	
    private int layerSize;

    public Haffman(int layerSize) {
        this.layerSize = layerSize;
    }

    private TreeSet<Neuron> set = new TreeSet<>();

    public Neuron make(Collection<Neuron> neurons) {
        set.addAll(neurons);
        while (set.size() > 1) {
        	try {
        		 merger();
			} catch (Exception e) {
				logger.error("{}", set.size());
				throw e;
			}
        }
        return set.first();
    }


    private void merger() {
        // TODO Auto-generated method stub
        NeuronHidden hn = new NeuronHidden(layerSize);
        Neuron min1 = set.pollFirst();
        Neuron min2 = set.pollFirst();
        hn.freq = min1.freq + min2.freq;
        min1.parent = hn;
        min2.parent = hn;
        min1.code = 0;
        min2.code = 1;
        set.add(hn);
    }
    
}
