package structures;

public class StringDoublePair {
	protected String s;
	protected double d;

	public StringDoublePair(String s, double d) {
		this.s = s;
		this.d = d;
	}

	public String getString() {
		return s;
	}

	public double getDouble() {
		return d;
	}

	@Override
	public String toString() {
		return s + "," + d;
	}

}
