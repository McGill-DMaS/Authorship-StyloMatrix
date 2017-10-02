package ca.mcgill.sis.dmas.nlp.model.embedding;

public class LongRandom {
	private long nextRandom = 1;

	public LongRandom(long seed) {
		this.nextRandom = seed;
	}

	public long nextRandom() {
		nextRandom = nextRandom * 25214903917L + 11;
		return nextRandom;
	}
}
