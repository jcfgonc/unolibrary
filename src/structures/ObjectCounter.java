package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Counts the number of "addings" of a given object (according to its hashcode).
 * 
 * @author CK
 *
 * @param <T>
 */
public class ObjectCounter<T> {

	Object2IntOpenHashMap<T> counter;

	public ObjectCounter() {
		counter = new Object2IntOpenHashMap<>();
	}

	public int addObject(T obj) {
		if (counter.containsKey(obj)) {
			return counter.addTo(obj, 1);
		} else {
			return counter.put(obj, 1);
		}
	}

	public int addObject(T obj, int count) {
		return counter.put(obj, count);
	}

	public int getCount(T obj) {
		return counter.getInt(obj);
	}

	public int getTotalCount() {
		int sum = 0;
		for (T key : counter.keySet()) {
			int count = counter.getInt(key);
			// if (count <= 2)
			// continue;
			sum += count;
		}
		return sum;
	}

	public void clear() {
		counter.clear();
	}

	public Set<T> keySet() {
		return counter.keySet();
	}

	public void toSystemOut() {
		toSystemOut(-Integer.MAX_VALUE);
	}

	public void toSystemOut(int lowLimit) {

		ArrayList<ObjectCount<T>> counts = getSortedCount();

		for (ObjectCount<T> count : counts) {
			if (count.getCount() < lowLimit)
				break;
			System.out.println(count);
		}
	}

	public ArrayList<ObjectCount<T>> getSortedCount() {
		ArrayList<ObjectCount<T>> counts = new ArrayList<>();
		for (T key : keySet()) {
			int count = getCount(key);
			counts.add(new ObjectCount<>(key, count));
		}
		counts.sort(null);
		return counts;
	}

	public void addObjects(Collection<T> objects) {
		for (T obj : objects) {
			addObject(obj);
		}
	}

}
