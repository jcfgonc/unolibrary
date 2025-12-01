package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Maps/indexes objects (according to their hashcode) to unique identifiers (numbers).
 *
 * @author Joao Goncalves: jcfgonc@gmail.com
 *
 * @param <T>
 */
public class ObjectIndex<T> {

	Int2ObjectOpenHashMap<T> idToObject;
	Object2IntOpenHashMap<T> objectToId;
	IntOpenHashSet freedIDs;

	public ObjectIndex() {
		idToObject = new Int2ObjectOpenHashMap<>();
		objectToId = new Object2IntOpenHashMap<>();
		freedIDs = new IntOpenHashSet();
	}

	public ObjectIndex(int size) {
		idToObject = new Int2ObjectOpenHashMap<>(size);
		objectToId = new Object2IntOpenHashMap<>(size);
		freedIDs = new IntOpenHashSet();
	}

	public ObjectIndex(Collection<T> values) {
		this(values.size() * 2);
		addAll(values);
	}

	public int add(T object) {
		return addObject(object);
	}

	public int addObject(T object) {
		int eid;
		if (objectToId.containsKey(object)) {
			eid = objectToId.getInt(object);
		} else {
			eid = objectToId.size();
			// recycle existing Ids
			if (!freedIDs.isEmpty()) {
				eid = freedIDs.iterator().nextInt(); // afaik no (external to the class) way of optimizing this
				freedIDs.remove(eid);
			}
			objectToId.put(object, eid);
			idToObject.put(eid, object);
		}
		return eid;
	}

	/**
	 * overwrites object/id
	 * 
	 * @param object
	 * @param eid
	 */
	public void addObject(T object, int eid) {
		objectToId.put(object, eid);
		idToObject.put(eid, object);
	}

	public int removeObject(T object) {
		int id = objectToId.removeInt(object);
		freedIDs.add(id);
		idToObject.remove(id);
		return id;
	}

	public void clear() {
		objectToId.clear();
		idToObject.clear();
	}

	public T getObject(int id) {
		return idToObject.get(id);
	}

	public int getObjectId(T object) {
		return objectToId.getInt(object);
	}

	@Override
	public String toString() {
		return idToObject.toString();
	}

	public boolean containsObject(T object) {
		return objectToId.containsKey(object);
	}

	public boolean containsID(int id) {
		return idToObject.containsKey(id);
	}

	public void addAll(Collection<T> objects) {
		addObjects(objects);
	}

	public void addObjects(Collection<T> objects) {
		for (T object : objects) {
			addObject(object);
		}
	}

	public Set<T> getObjects() {
		return objectToId.keySet();
	}

	public void toSystemOut() {
		ArrayList<Entry<T>> entries = new ArrayList<>();
		// create a list of object-index pairs
		for (Entry<T> entry : objectToId.object2IntEntrySet()) {
			entries.add(entry);
		}
		Collections.sort(entries, new Comparator<Entry<T>>() {
			@Override
			public int compare(Entry<T> o1, Entry<T> o2) {
				// sorting in descending order
				return Integer.compare(o2.getIntValue(), o1.getIntValue());
			}
		});
		for (Entry<T> entry : entries) {
			System.out.printf("%d\t%s\n", entry.getIntValue(), entry.getKey());
		}
	}

}
