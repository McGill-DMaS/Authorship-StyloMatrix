package ca.mcgill.sis.dmas.io.collection;

public class Counter {
	public volatile int count = 0;

	public void inc() {
		count++;
	}

	public int incRO() {
		count++;
		return count - 1;
	}

	public void inc(int val) {
		count += val;
	}

	public double percentage(int total) {
		return count * 1.0 / total;
	}

	public void dec(int val) {
		count -= val;
	}

	public void dec() {
		count--;
	}

	public int getVal() {
		return count;
	}

	public static Counter zero() {
		return new Counter();
	}
}
