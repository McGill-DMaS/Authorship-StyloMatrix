package ca.mcgill.sis.dmas.io.collection;

public class Switch {

	public boolean value = false;

	public void flip() {
		value ^= value;
	}

	public boolean getVal() {
		return value;
	}

	public Switch(boolean value) {
		this.value = value;
	}

}
