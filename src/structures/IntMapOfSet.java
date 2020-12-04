package structures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * A Map where each mapping value is a Set of the given type, mapped to by an integer key. Created for convenience.
 * 
 * @author João Gonçalves: jcfgonc@gmail.com
 * @param <K>
 * @param <V>
 */
public class IntMapOfSet<V> {

	private Int2ObjectOpenHashMap<Set<V>> map;

	public IntMapOfSet(int a) {
		map = new Int2ObjectOpenHashMap<Set<V>>(a);
	}

	public IntMapOfSet() {
		map = new Int2ObjectOpenHashMap<Set<V>>();
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(int key) {
		return map.containsKey(key);
	}

	public Set<V> get(int key) {
		return map.get(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public IntSet keySet() {
		return map.keySet();
	}

	public Set<V> mergeSets() {
		HashSet<V> merged = new HashSet<>();
		Collection<Set<V>> vSet = map.values();
		for (Set<V> set : vSet) {
			merged.addAll(set);
		}
		return merged;
	}

	public boolean put(int key, V value) {
		Set<V> set = get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		return set.add(value);
	}

	public void put(int key, Collection<V> values) {
		for (V value : values) {
			put(key, value);
		}
	}

	public void removeFromValues(Collection<V> values) {
		for (V value : values) {
			removeFromValues(value);
		}
	}

	public void removeFromValues(V value) {
		// iterate through every mapped set (target)
		for (Set<V> values : this.values()) {
			values.remove(value);
		}
	}

	public Set<V> removeKey(int key) {
		return map.remove(key);
	}

	public int size() {
		return map.size();
	}

	public String toString() {
		return map.toString();
	}

	public Collection<Set<V>> values() {
		return map.values();
	}

}
