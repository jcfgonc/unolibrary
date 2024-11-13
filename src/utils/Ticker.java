package utils;

/**
 * Class to manage elapsed time.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 * 
 */
public class Ticker {

	private long s2;
	private double time_lastcall;

	/**
	 * Creates a new ticker, sets its timer to zero and starts counting.
	 */
	public Ticker() {
		this.resetTicker();
		time_lastcall = 0;
	}

	/**
	 * Returns the elapsed time (in seconds) since the creation (or reset) of this ticker.
	 * 
	 * @return
	 */
	public double getElapsedTime() {
		double dif = getNanoTime() - s2;
		double el = dif / 1000000000.0;
		return el;
	}

	private long getNanoTime() {
		return System.nanoTime();
	}

	/**
	 * Returns the elapsed time (in seconds) this last call.
	 * 
	 * @return
	 */
	public double getTimeDeltaLastCall() {
		double t1 = this.getElapsedTime();
		double tdif = t1 - time_lastcall;
		time_lastcall = t1;
		return tdif;
	}

	/**
	 * Sets this ticker timer to zero and starts counting again.
	 */
	public void resetTicker() {
		s2 = getNanoTime();
	}

}
