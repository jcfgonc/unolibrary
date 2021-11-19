package structures;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * A Synchronized/Concurrent Map where each mapping is a Set of the given type, mapped to by a key. Encapsulates MapOfSet with synchronized blocks.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 * @param <K>
 * @param <V>
 */
public class SynchronizedMapOfSet<K, V> extends MapOfSet<K, V> implements Serializable {
	private static final long serialVersionUID = -671719707013812287L;

	public synchronized void add(K key, Collection<V> values) {
		super.add(key, values);
	}

	public synchronized boolean add(K key, V value) {
		return super.add(key, value);
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
		return super.get(key);
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
