package structures;

import org.apache.commons.math3.random.RandomGenerator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FixedIntegerRandomGenerator implements RandomGenerator {

	private IntArrayList values;
	private int position;

	public FixedIntegerRandomGenerator(IntArrayList values) {
		this.values = values;
		this.position = 0;
	}

	public IntArrayList getValues() {
		return values;
	}

	public void setValues(IntArrayList values) {
		this.values = values;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public void setSeed(int seed) {
		if (seed > values.size()) {
			this.position = seed % values.size();
		} else {
			this.position = seed;
		}
	}

	@Override
	public int nextInt() {
		int val = values.getInt(position);
		position++;
		return val;
	}

	@Override
	public int nextInt(int n) {
		int val = nextInt();
		if (val > n) {
			return val % n;
		} else {
			return val;
		}
	}

	@Override
	/**
	 * UNUSED
	 */
	public void setSeed(int[] seed) {
	}

	@Override
	/**
	 * UNUSED
	 */
	public void setSeed(long seed) {
		setSeed((int) seed);
	}

	@Override
	/**
	 * UNUSED
	 */
	public void nextBytes(byte[] bytes) {
	}

	@Override
	/**
	 * UNUSED
	 */
	public long nextLong() {
		return 0;
	}

	@Override
	/**
	 * UNUSED
	 */
	public boolean nextBoolean() {
		return false;
	}

	@Override
	/**
	 * UNUSED
	 */
	public float nextFloat() {
		return 0;
	}

	@Override
	/**
	 * UNUSED
	 */
	public double nextDouble() {
		return 0;
	}

	@Override
	/**
	 * UNUSED
	 */
	public double nextGaussian() {
		return 0;
	}

}
