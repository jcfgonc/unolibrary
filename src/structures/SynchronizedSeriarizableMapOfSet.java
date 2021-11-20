package structures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

/**
 * A Synchronized/Concurrent Map where each mapping is a Set of the given type, mapped to by a key. Encapsulates MapOfSet with synchronized blocks.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 * @param <K>
 * @param <V>
 */
public class SynchronizedSeriarizableMapOfSet<K, V> extends MapOfSet<K, V> implements Serializable {
	private static final long serialVersionUID = -671719707013812287L;

	private String filename;
	private Ticker ticker;
	private int timeout;
	private ReentrantReadWriteLock rrw;
	private static Kryo kryo; // must be shared
	private boolean fileSynch;

	public SynchronizedSeriarizableMapOfSet(String filename, int timeout) {
		super();
		this.filename = filename;
		this.ticker = new Ticker();
		this.timeout = timeout;
		this.rrw = new ReentrantReadWriteLock();
		fileSynch = true;
		initializeKryo();
		System.err.printf("%s: saving to %s with an interval of %ds\n", this.getClass().toString(), filename, timeout);
		load();
	}

	public SynchronizedSeriarizableMapOfSet() {
		super();
		this.ticker = new Ticker();
		this.rrw = new ReentrantReadWriteLock();
		fileSynch = false;
	}

	private synchronized void initializeKryo() {
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.register(ConcurrentHashMap.class);
		kryo.register(HashSet.class);
	}

	@SuppressWarnings({ "unchecked" })
	public synchronized void load() {
		if (!fileSynch)
			return;

		File fin = new File(filename);
		if (fin.exists()) {
			try {
				ticker.getTimeDeltaLastCall();

				Input input = new Input(new FileInputStream(filename));
				super.map = (HashMap<K, Set<V>>) kryo.readObject(input, HashMap.class);
				input.close();

				double dt = ticker.getTimeDeltaLastCall();
				System.err.println(this.getClass().toString() + ": loaded cache from " + filename + " with " + size() + " entries in " + dt + "s");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-3);
			}
		}
	}

	public synchronized void save() throws IOException {
		if (!fileSynch)
			return;

		try {
			Output output = new Output(new FileOutputStream(filename));
			kryo.writeObject(output, super.map);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void add(K key, Collection<V> values) {
		rrw.writeLock().lock();
		super.add(key, values);
		rrw.writeLock().unlock();

		if (fileSynch) {
			checkTimeout();
		}
	}

	public synchronized boolean add(K key, V value) {
		rrw.writeLock().lock();
		boolean b = super.add(key, value);
		rrw.writeLock().unlock();

		if (fileSynch) {
			checkTimeout();
		}
		return b;
	}

	private synchronized void checkTimeout() {
		if (ticker.getElapsedTime() > timeout) {
			try {

				// ticker.getTimeDeltaLastCall();

				// cache can not be updated while saving it (block writes)
				// rrw.readLock().lock();
				save();
				// rrw.readLock().unlock();

				// double dt = ticker.getTimeDeltaLastCall();
				// System.err.println(this.getClass().toString() + ": saved cache to " + filename + " with " + size() + " entries in " + dt + "s");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ticker.resetTicker();
		}
	}

	public synchronized void clear() {
		super.clear();
	}

	public synchronized boolean containsKey(K key) {
		return super.containsKey(key);
	}

	/**
	 * Returns a *view* of the set mapped to from the given key.
	 * 
	 * @param key
	 * @return
	 */
	public synchronized Set<V> get(K key) {
		rrw.readLock().lock();
		Set<V> set = super.get(key);
		rrw.readLock().unlock();
		return set;
	}

	public synchronized boolean isEmpty() {
		return super.isEmpty();
	}

	public synchronized Set<K> keySet() {
		return super.keySet();
	}

	public synchronized Set<V> mergeSets() {
		return super.mergeSets();
	}

	/**
	 * Removes from the set mapped to the given key the given value
	 * 
	 * @param target
	 * @param edge
	 * @return
	 */
	public synchronized boolean remove(K key, V value) {
		return super.remove(key, value);
	}

	/**
	 * Removes from the set mapped to the given key the given values
	 * 
	 * @param target
	 * @param edge
	 * @return
	 */
	public synchronized void remove(K key, Collection<V> values) {
		super.remove(key, values);
	}

	public synchronized void removeFromValues(Collection<V> values) {
		super.removeFromValues(values);
	}

	public synchronized void removeFromValues(V value) {
		super.removeFromValues(value);
	}

	public synchronized Set<V> removeKey(K key) {
		return super.removeKey(key);
	}

	public synchronized int size() {
		return super.size();
	}

	public synchronized String toString() {
		return super.toString();
	}

	public synchronized Collection<Set<V>> values() {
		return super.values();
	}
}
