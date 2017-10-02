package ca.mcgill.sis.dmas.nlp.model.embedding;


public class NeuronHidden extends Neuron{
    
    public double[] syn1 ; //hidden->out
    public int index;
    
    public NeuronHidden(int layerSize){
        syn1 = new double[layerSize] ;
    }
    
    public NeuronHidden(int index, double[] vector){
        syn1 = vector;
        this.index = index;
    }
    
}
