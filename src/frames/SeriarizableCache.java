package frames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import structures.Ticker;

public class SeriarizableCache<K, V> {

	private ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<K, V>();
	private String filename;
	private Ticker ticker;
	private int timeout;
	private ReentrantReadWriteLock rrw;
	private ReentrantLock filesaveLock;

	public SeriarizableCache(String filename, int timeout) {
		this.filename = filename;
		this.ticker = new Ticker();
		this.timeout = timeout;
		this.rrw = new ReentrantReadWriteLock();
		this.filesaveLock = new ReentrantLock();
		System.err.printf("saving SeriarizableCache() to %s with an interval of %ds\n", filename, timeout);
		reload();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void reload() {
		File fin = new File(filename);
		if (fin.exists()) {
			try {
				FileInputStream file = new FileInputStream(filename);
				ObjectInputStream in = new ObjectInputStream(file);

				cache = (ConcurrentHashMap) in.readObject();

				in.close();
				file.close();

				System.err.println("loaded cache from " + filename + " with " + size() + " entries");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-3);
			}
		}
	}

	public synchronized void save() throws IOException {
//		System.out.println("saving SeriarizableCache");
		FileOutputStream file = new FileOutputStream(filename);
		ObjectOutputStream out = new ObjectOutputStream(file);

		// Method for serialization of object
		out.writeObject(cache);

		out.close();
		file.close();

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

	private void checkTimeout() {
		// only one file save at a time
		filesaveLock.lock();

		if (ticker.getElapsedTime() > timeout) {
			try {

				// cache can not be updated while saving it (block writes)
				rrw.readLock().lock();
				save();
				rrw.readLock().unlock();

				System.err.println("saved cache to " + filename + " with " + size() + " entries");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ticker.resetTicker();
		}

		filesaveLock.unlock();
	}
}
