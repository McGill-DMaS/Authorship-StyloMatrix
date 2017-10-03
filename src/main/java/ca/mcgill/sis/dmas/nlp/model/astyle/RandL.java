package ca.mcgill.sis.dmas.nlp.model.astyle;

public class RandL {
	public long nextRandom = 1;

	public RandL(long seed) {
		this.nextRandom = seed;
	}

	public long nextR() {
		nextRandom = nextRandom * 25214903917L + 11;
		return nextRandom;
	}

	public int nextResidue(int max) {
		return (int) Long.remainderUnsigned(this.nextR(), max);
	}

	public double nextF() {
		double val = 
				((this.nextR() & 0xFFFF) / 65536d);
		return val;
	}

	public static void main(String[] args) {
		RandL rl = new RandL(0);
		for (int i = 0; i < 1000; ++i) {
			System.out.println(rl.nextF());
		}
	}
}
