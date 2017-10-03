package ca.mcgill.sis.dmas.nlp.model.embedding;

public class RandomL {
	private double rand;
	public RandomL(int seed){
		rand = seed;
	}
	
	public double next(){
		rand = rand * 25214903917L + 11;
		return rand;
	}
}
