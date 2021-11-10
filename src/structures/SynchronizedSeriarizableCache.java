package structures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

public class SynchronizedSeriarizableCache<K, V> {

	private ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<K, V>();
	private String filename;
	private Ticker ticker;
	private int timeout;
	private ReentrantReadWriteLock rrw;
	private static Kryo kryo; // must be shared

	public SynchronizedSeriarizableCache(String filename, int timeout) {
		this.filename = filename;
		this.ticker = new Ticker();
		this.timeout = timeout;
		this.rrw = new ReentrantReadWriteLock();
		initializeKryo();
		System.err.printf("%s: saving to %s with an interval of %ds\n", this.getClass().toString(), filename, timeout);
		load();
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
				cache = (ConcurrentHashMap) kryo.readObject(input, ConcurrentHashMap.class);
				input.close();

//				FileInputStream file = new FileInputStream(filename);
//				ObjectInputStream in = new ObjectInputStream(file);
//				cache = (ConcurrentHashMap) in.readObject();
//				in.close();
//				file.close();

				double dt = ticker.getTimeDeltaLastCall();
				System.err.println(this.getClass().toString() + ": loaded cache from " + filename + " with " + size() + " entries in " + dt + "s");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-3);
			}
		}
	}

	public synchronized void save() throws IOException {

		try {
			Output output = new Output(new FileOutputStream(filename));
			kryo.writeObject(output, cache);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println("saving SeriarizableCache");
//		FileOutputStream file = new FileOutputStream(filename);
//		ObjectOutputStream out = new ObjectOutputStream(file);
//
//		// Method for serialization of object
//		out.writeObject(cache);
//
//		out.close();
//		file.close();

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

	private synchronized void checkTimeout() {
		if (ticker.getElapsedTime() > timeout) {
			try {

				ticker.getTimeDeltaLastCall();

				// cache can not be updated while saving it (block writes)
				rrw.readLock().lock();
				save();
				rrw.readLock().unlock();

				double dt = ticker.getTimeDeltaLastCall();
				System.err.println(this.getClass().toString() + ": saved cache to " + filename + " with " + size() + " entries in " + dt + "s");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ticker.resetTicker();
		}
	}
}