package structures;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class to manage elapsed time. When I first started this class (some 10 years ago) I didn't knew about Apache's StopWatch.
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

	ReentrantReadWriteLock updateLock;

	/**
	 * Creates a new ticker, sets its timer to zero and starts counting.
	 */
	public Ticker() {
		updateLock = new ReentrantReadWriteLock();
		resetTicker();
//		getTimeDeltaLastCall();
	}

	/**
	 * Returns the elapsed time (in seconds) since the creation (or reset) of this ticker.
	 * 
	 * @return
	 */
	public double getElapsedTime() {
		double dif = 0;
		updateLock.readLock().lock();
		{
			dif = Math.abs(getTime() - reference);
		}
		updateLock.readLock().unlock();
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
		double tdif = 0;
		updateLock.writeLock().lock();
		{
			double t1 = getTime();
			tdif = t1 - time_lastcall;
			time_lastcall = t1;
		}
		updateLock.writeLock().unlock();
		return tdif;
	}

	/**
	 * Sets this ticker timer to zero and starts counting again.
	 */
	public void resetTicker() {
		updateLock.writeLock().lock();
		{
			reference = getTime();
			time_lastcall = reference;
		}
		updateLock.writeLock().unlock();
	}

	public void showTimeDeltaLastCall() {
		System.out.println("d(t)=" + getTimeDeltaLastCall());
	}

}
