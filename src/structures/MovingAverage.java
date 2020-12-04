package structures;

public class MovingAverage {
	int n;
	double[] a;
	double sum;
	int i;

	public MovingAverage(int windowSize) {
		this.n = windowSize;
		a = new double[windowSize];
		i = 0;
		sum = 0;
	}

	public int limit(int pos, int max) {
		return (pos < max) ? pos : 0;
	}

	public void add(double val) {
		i = limit(i, n);
		sum -= a[i];
		a[i] = val;
		sum += a[i];
		i++;
	}

	public double getMean() {
		return (double) sum / n;
	}
}
