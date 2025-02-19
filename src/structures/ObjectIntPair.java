package structures;

/**
 * Class associating an object to an integer, i.e., an object and its count. To be used as a pair "<T>, int" representing the number of elements T present in
 * some structure.
 * 
 * @author CK
 *
 * @param <T>
 */
public class ObjectIntPair<T> implements Comparable<ObjectIntPair<T>> {
	private int count;
	private T id;

	public ObjectIntPair(T id, int count) {
		super();
		this.id = id;
		this.count = count;
	}

	@Override
	public int compareTo(ObjectIntPair<T> o) {
		return Integer.compare(o.count, count); // decreasing
	}

	public int getCount() {
		return count;
	}

	public T getId() {
		return id;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setId(T id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id + "\t" + count;
	}

}
