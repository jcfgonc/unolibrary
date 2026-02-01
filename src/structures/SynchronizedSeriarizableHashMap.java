package structures;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import utils.VariousUtils;

public class SynchronizedSeriarizableHashMap<K, V> {

	private ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<K, V>();
	private String filename;
	private Ticker saveTicker;
	private int timeout;
	private ReentrantReadWriteLock rrw;
	private Semaphore savingSemaphore = new Semaphore(1);
	private Semaphore timeoutSemaphore = new Semaphore(1);

	public SynchronizedSeriarizableHashMap(String filename, int timeout) {
		this.filename = filename;
		this.saveTicker = new Ticker();
		this.timeout = timeout;
		this.rrw = new ReentrantReadWriteLock();
		load();
	}

	public SynchronizedSeriarizableHashMap(String filename) {
		this(filename, 10);
	}

	public boolean containsKey(Object key) {
		boolean contains = false;
		try {
			rrw.readLock().lock();
			contains = cache.containsKey(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rrw.readLock().unlock();
		}
		return contains;
	}

	public boolean isEmpty() {
		boolean isEmpty = false;
		try {
			rrw.readLock().lock();
			isEmpty = cache.isEmpty();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rrw.readLock().unlock();
		}
		return isEmpty;
	}

	public boolean containsValue(V value) {
		boolean contains = false;
		try {
			rrw.readLock().lock();
			contains = cache.containsValue(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rrw.readLock().unlock();
		}
		return contains;
	}

	@SuppressWarnings({ "unchecked" })
	public void load() {
		File f = new File(filename);
		if (f.exists() && !f.isDirectory()) {
			try {
				rrw.writeLock().lock();
				cache = (ConcurrentHashMap<K, V>) VariousUtils.loadObjectFromFile(filename);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); // serious when loading cache
			} finally {
				rrw.writeLock().unlock();
			}
			System.err.printf("warning: loaded map with %d entries from %s\n", cache.size(), filename);
		} else {
			System.err.printf("warning: filename %s does not exist\n", filename);
		}
	}

	public long size() {
		int s = -1;
		try {
			rrw.readLock().lock();
			s = cache.size();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rrw.readLock().unlock();
		}
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

		saveTimeout();

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
					VariousUtils.saveObjectToFile(cache, filename);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); // serious when saving cache
			} finally {
				rrw.readLock().unlock();
				savingSemaphore.release();
			}
		}
		System.err.printf("warning: saved map with %d entries to %s\n", cache.size(), filename);
	}

	private void saveTimeout() {
		// prevent concurrent timeouts
		try {
			if (timeoutSemaphore.tryAcquire(0, TimeUnit.SECONDS)) {
				if (saveTicker.getElapsedTime() > timeout) {
					try {
						save();
					} catch (IOException e) {
						e.printStackTrace();
					}
					saveTicker.resetTicker();
				}
				timeoutSemaphore.release();
			}
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
