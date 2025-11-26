package structures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

public class SynchronizedSeriarizableHashMap<K, V> {

	private ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<K, V>();
	private String filename;
	private Ticker ticker;
	private int timeout;
	private ReentrantReadWriteLock rrw;
	private static Kryo kryo; // must be shared
	private Semaphore savingSemaphore = new Semaphore(1);
	private Semaphore timeoutSemaphore = new Semaphore(1);

	public SynchronizedSeriarizableHashMap(String filename, int timeout) {
		this.filename = filename;
		this.ticker = new Ticker();
		this.timeout = timeout;
		this.rrw = new ReentrantReadWriteLock();
		initializeKryo();
//		System.err.printf("%s: saving to %s with an interval of %ds\n", this.getClass().toString(), filename, timeout);
		load();
	}

	public SynchronizedSeriarizableHashMap(String filename) {
		this(filename, 10);

	}

	public synchronized boolean containsKey(Object key) {
		return cache.containsKey(key);
	}

	public synchronized boolean isEmpty() {
		return cache.isEmpty();
	}

	public synchronized boolean containsValue(V value) {
		return cache.containsValue(value);
	}

	private synchronized void initializeKryo() {
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.register(ConcurrentHashMap.class);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void load() {
		File fin = new File(filename);
		if (fin.exists()) {
			try {
				ticker.getTimeDeltaLastCall();

				Input input = new Input(new FileInputStream(filename));
				rrw.writeLock().lock();
				cache = (ConcurrentHashMap) kryo.readObject(input, ConcurrentHashMap.class);
				rrw.writeLock().unlock();
				input.close();

				// double dt = ticker.getTimeDeltaLastCall();
				// System.err.println(this.getClass().toString() + ": loaded cache from " + filename + " with " + size() + " entries in " + dt + "s");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public long size() {
		rrw.readLock().lock();
		int s = cache.size();
		rrw.readLock().unlock();

		return s;
	}

	public V get(K key) {
		rrw.readLock().lock();
		V v = (V) cache.get(key);
		rrw.readLock().unlock();

		return v;
	}

	public V put(K key, V value) {
		rrw.writeLock().lock();
		V p = (V) cache.put(key, value);
		rrw.writeLock().unlock();

		checkTimeout();

		return p;
	}

	public void save() throws IOException {
		// protect saving function
		if (savingSemaphore.tryAcquire()) {
			try {
				// cache can not be updated while saving it (block writes)
				rrw.readLock().lock();
				// do the save
				{
					Output output = new Output(new FileOutputStream(filename));
					kryo.writeObject(output, cache);
					output.flush();
					output.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				rrw.readLock().unlock();
			}
		}
		savingSemaphore.release();
		System.err.printf("warning: saved cache with %d entries to %s\n", cache.size(), filename);
	}

	private void checkTimeout() {
		// prevent concurrent timeouts
		try {
			if (timeoutSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
				if (ticker.getElapsedTime() > timeout) {
					try {
						save();
					} catch (IOException e) {
						e.printStackTrace();
					}
					ticker.resetTicker();
				}
			}
			timeoutSemaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void debug() {
		Enumeration<V> elements = cache.elements();
		while (elements.hasMoreElements()) {
			V nextElement = elements.nextElement();
			String str = nextElement.toString();
			System.out.println(str);
//			if (str.contains("→")) {
//				System.out.println(str);
//			}
		}
	}
}
