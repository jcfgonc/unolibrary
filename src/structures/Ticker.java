package structures;

/**
 * Class to manage elapsed time
 * 
 * @author jcfgonc@gmail.com
 * 
 */
public class Ticker {

	/**
	 * reference time, set at class creation time
	 */
	private double reference;
	/**
	 * time when getTimeDeltaLastCall() was invoked
	 */
	private double time_lastcall;

	/**
	 * Creates a new ticker, sets its timer to zero and starts counting.
	 */
	public Ticker() {
		resetTicker();
//		getTimeDeltaLastCall();
	}

	/**
	 * Returns the elapsed time (in seconds) since the creation (or reset) of this ticker.
	 * 
	 * @return
	 */
	public double getElapsedTime() {
		double dif = getTime() - reference;
		return dif;
	}

	/**
	 * Returns the current value of the running Java Virtual Machine'shigh-resolution time source, in seconds.
	 * 
	 * @return
	 */
	private double getTime() {
		return System.nanoTime() * 1e-9;
	}

	/**
	 * Returns the elapsed time (in seconds) this last call.
	 * 
	 * @return
	 */
	public double getTimeDeltaLastCall() {
		double t1 = getTime();
		double tdif = t1 - time_lastcall;
		time_lastcall = t1;
		return tdif;
	}

	/**
	 * Sets this ticker timer to zero and starts counting again.
	 */
	public void resetTicker() {
		reference = getTime();
		time_lastcall = reference;
	}

	public void showTimeDeltaLastCall() {
		System.out.printf("d(t)=%f\n", getTimeDeltaLastCall());
	}

}
