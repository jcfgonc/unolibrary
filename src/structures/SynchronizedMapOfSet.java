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

	public SynchronizedMapOfSet(int initialCapacity, float loadFactor) {
		this.initialCapacity = initialCapacity;
		this.loadFactor = loadFactor;
		map = new HashMap<K, Set<V>>(initialCapacity, loadFactor);
	}

	public SynchronizedMapOfSet(int initialCapacity) {
		this(initialCapacity, 0.5f);
	}

	public SynchronizedMapOfSet() {
		this(16, 0.5f);
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

	public synchronized boolean add(K key, V value) {
		Set<V> set = get(key);
		if (set == null) {
			set = new HashSet<V>(initialCapacity, loadFactor);
			map.put(key, set);
		}
		return set.add(value);
	}

	public synchronized void add(K key, Collection<V> values) {
		for (V value : values) {
			add(key, value);
		}
	}

	public synchronized void removeFromValues(Collection<V> values) {
		for (V value : values) {
			removeFromValues(value);
		}
	}

	public synchronized void removeFromValues(V value) {
		// iterate through every mapped set (target)
		for (Set<V> values : this.values()) {
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
