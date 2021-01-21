package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * A Map where each mapping is a Set of the given type, mapped to by a key. Created for convenience.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 * @param <K>
 * @param <V>
 */
public class MapOfList<K, V> {

	private HashMap<K, List<V>> map;

	public MapOfList() {
		map = new HashMap<K, List<V>>(16, 0.333333f);
	}

	public void put(K key, Collection<V> values) {
		for (V value : values) {
			add(key, value);
		}
	}

	public boolean add(K key, V value) {
		List<V> list = get(key);
		if (list == null) {
			list = new ArrayList<V>();
			map.put(key, list);
		}
		return list.add(value);
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public List<V> get(K key) {
		return map.get(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public List<V> remove(K key) {
		return map.remove(key);
	}

	public int size() {
		return map.size();
	}

	public String toString() {
		return map.toString();
	}

	public Collection<List<V>> values() {
		return map.values();
	}

	public List<V> mergeLists() {
		ArrayList<V> merged = new ArrayList<>();
		Collection<List<V>> vList = map.values();
		for (List<V> list : vList) {
			merged.addAll(list);
		}
		return merged;
	}

	public void removeFromValues(V value) {
		// iterate through every mapped list (target)
		for (List<V> values : this.values()) {
			values.remove(value);
		}
	}

	public void removeFromValues(Collection<V> values) {
		for (V value : values) {
			removeFromValues(value);
		}
	}

}
