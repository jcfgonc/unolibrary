package structures;

/**
 * Class associating an object to a number, i.e., an object and its count. To be used as a pair "<T>, int" representing the number of elements T present in some structure.
 * 
 * @author CK
 *
 * @param <T>
 */
public class ObjectCount<T> implements Comparable<ObjectCount<T>> {
	private int count;
	private T id;

	public ObjectCount(T id, int count) {
		super();
		this.id = id;
		this.count = count;
	}

	@Override
	public int compareTo(ObjectCount<T> o) {
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
