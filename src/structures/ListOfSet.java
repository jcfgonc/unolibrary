package structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * An array where each element is a set of the given type. Created for convenience.
 * 
 * @author Joao Goncalves: jcfgonc@gmail.com
 *
 */
public class ListOfSet<E> implements Iterable<HashSet<E>> {

	private ArrayList<HashSet<E>> array;

	public ListOfSet() {
		super();
		array = new ArrayList<>();
	}

	public ListOfSet(ListOfSet<E> other) {
		super();
		array = new ArrayList<>(other.array.size());
		addAll(other);
	}

	public void addAll(ListOfSet<E> other) {
		for (int i = 0; i < other.size(); i++) {
			HashSet<E> set = other.getSetAt(i);
			add(set);
		}
	}

	/**
	 * creates a new, empty set at the given position
	 * 
	 * @param pos
	 * @return
	 */
	public HashSet<E> createSetAt(int pos) {
		fillTo(pos);
		HashSet<E> set = getSetAt(pos);
		return set;
	}

	/**
	 * fills the array with empty sets until the given position
	 * 
	 * @param pos
	 */
	private void fillTo(int pos) {
		int size = this.size();
		for (int i = size; i <= pos; i++) {
			HashSet<E> existing = getSetAt(i);
			if (existing == null) {
				existing = new HashSet<>();
				array.add(existing);
			}
		}
	}

	/**
	 * adds the given element to the set at the requested position
	 * 
	 * @param e
	 * @param pos
	 * @return
	 */
	public boolean addElementToSetAt(E e, int pos) {
		HashSet<E> set = this.getSetAt(pos);
		if (set == null)
			set = createSetAt(pos);
		return set.add(e);
	}

	/**
	 * adds the given set to the last position
	 * 
	 * @param e
	 * @return
	 */
	public boolean add(HashSet<E> e) {
		return array.add(e);
	}

	/**
	 * checks if any set contains the given element
	 * 
	 * @param o
	 * @return
	 */
	public boolean contains(Object o) {
		for (HashSet<E> set : array) {
			if (set.contains(o))
				return true;
		}
		return false;
	}

	public HashSet<E> getSetAt(int index) {
		return array.get(index);
	}

	public boolean isEmpty() {
		return array.isEmpty();
	}

	/**
	 * this iterator iterates over each set
	 * 
	 * @return
	 */
	public Iterator<HashSet<E>> iterator() {
		return array.iterator();
	}

	public int size() {
		return array.size();
	}

	public String toString() {
		return array.toString();
	}

	/**
	 * sorts the list of sets, IN-PLACE
	 * 
	 * @param ascending
	 */
	public void sortList(boolean ascending) {
		array.sort(new Comparator<HashSet<E>>() {

			@Override
			public int compare(HashSet<E> o1, HashSet<E> o2) {
				int compare = Integer.compare(o1.size(), o2.size());
				if (ascending) {
					return compare;
				} else {
					return -compare;
				}
			}

		});
	}

	/**
	 * returns a random set
	 * 
	 * @param random
	 * @return
	 */
	public HashSet<E> getRandomSet(RandomGenerator random) {
		int size = array.size();

		assert size > 0;

		int pos = random.nextInt(size);
		return array.get(pos);
	}
}
