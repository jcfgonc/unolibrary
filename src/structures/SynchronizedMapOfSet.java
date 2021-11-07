package structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A Map where each mapping is a Set of the given type, mapped to by a key. Created for convenience.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 * @param <K>
 * @param <V>
 */
public class SynchronizedMapOfSet<K, V> {
	private final int initialCapacity;
	private final float loadFactor;
	private HashMap<K, Set<V>> map;

	public SynchronizedMapOfSet() {
		this(16, 0.5f);
	}

	public SynchronizedMapOfSet(int initialCapacity) {
		this(initialCapacity, 0.5f);
	}

	public SynchronizedMapOfSet(int initialCapacity, float loadFactor) {
		this.initialCapacity = initialCapacity;
		this.loadFactor = loadFactor;
		map = new HashMap<K, Set<V>>(initialCapacity, loadFactor);
	}

	public synchronized void add(K key, Collection<V> values) {
		for (V value : values) {
			add(key, value);
		}
	}

	public synchronized boolean add(K key, V value) {
		if (key == null || value == null)
			throw new RuntimeException("SynchronizedMapOfSet: trying to add " + key + "," + value);

//		System.out.printf("SynchronizedMapOfSet.add(%s,%s)\n", key, value);

		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>(initialCapacity, loadFactor);
			map.put(key, set);
		}
		return set.add(value);
	}

	public synchronized void clear() {
		map.clear();
	}

	public synchronized boolean containsKey(K key) {
		Set<V> set = map.get(key);
		return set != null && !set.isEmpty();
	}

	public synchronized Set<V> get(K key) {
		Set<V> s = map.get(key);
		if (s == null || s.isEmpty())
			return null;
		return Collections.unmodifiableSet(s);
	}

	public synchronized boolean isEmpty() {
		return map.isEmpty();
	}

	public synchronized Set<K> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	public synchronized Set<V> mergeSets() {
		HashSet<V> merged = new HashSet<>(initialCapacity, loadFactor);
		Collection<Set<V>> vSet = map.values();
		for (Set<V> set : vSet) {
			merged.addAll(set);
		}
		return merged;
	}

	/**
	 * Removes from the set mapped to the given key the given value
	 * 
	 * @param target
	 * @param edge
	 * @return
	 */
	public synchronized boolean remove(K key, V value) {
		return map.get(key).remove(value);
	}

	/**
	 * Removes from the set mapped to the given key the given values
	 * 
	 * @param target
	 * @param edge
	 * @return
	 */
	public synchronized void remove(K key, Collection<V> values) {
		Set<V> set = map.get(key);
		for (V value : values) {
			set.remove(value);
		}
	}

	public synchronized void removeFromValues(Collection<V> values) {
		for (V value : values) {
			removeFromValues(value);
		}
	}

	public synchronized void removeFromValues(V value) {
		// iterate through every mapped set (target)
		for (Set<V> values : map.values()) {
			values.remove(value);
		}
	}

	public synchronized Set<V> removeKey(K key) {
		return map.remove(key);
	}

	public synchronized int size() {
		return map.size();
	}

	public synchronized String toString() {
		return map.toString();
	}

	public synchronized Collection<Set<V>> values() {
		return Collections.unmodifiableCollection(map.values());
	}
}
