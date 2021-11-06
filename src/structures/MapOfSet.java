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
public class MapOfSet<K, V> {
	private final int initialCapacity;
	private final float loadFactor;
	private HashMap<K, Set<V>> map;

	public MapOfSet(int initialCapacity, float loadFactor) {
		this.initialCapacity = initialCapacity;
		this.loadFactor = loadFactor;
		map = new HashMap<K, Set<V>>(initialCapacity, loadFactor);
	}

	public MapOfSet(int initialCapacity) {
		this(initialCapacity, 0.5f);
	}

	public MapOfSet() {
		this(16, 0.5f);
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(K key) {
		Set<V> set = map.get(key);
		return set != null && !set.isEmpty();
	}

	public Set<V> get(K key) {
		Set<V> s = map.get(key);
		if (s == null || s.isEmpty())
			return null;
		return Collections.unmodifiableSet(s);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	public Set<V> mergeSets() {
		HashSet<V> merged = new HashSet<>(initialCapacity, loadFactor);
		Collection<Set<V>> vSet = map.values();
		for (Set<V> set : vSet) {
			merged.addAll(set);
		}
		return merged;
	}

	public boolean add(K key, V value) {
		if (key == null || value == null)
			throw new RuntimeException("MapOfSet: trying to add " + key + "," + value);

		System.out.printf("MapOfSet.add(%s,%s)\n", key, value);

		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>(initialCapacity, loadFactor);
			map.put(key, set);
		}
		return set.add(value);
	}

	public void add(K key, Collection<V> values) {
		for (V value : values) {
			add(key, value);
		}
	}

	public void removeFromValues(Collection<V> values) {
		for (V value : values) {
			removeFromValues(value);
		}
	}

	public void removeFromValues(V value) {
		// iterate through every mapped set (target)
		for (Set<V> values : map.values()) {
			values.remove(value);
		}
	}

	public Set<V> removeKey(K key) {
		return map.remove(key);
	}

	public int size() {
		return map.size();
	}

	public String toString() {
		return map.toString();
	}

	public Collection<Set<V>> values() {
		return Collections.unmodifiableCollection(map.values());
	}

}
