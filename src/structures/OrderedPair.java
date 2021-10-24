package structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Two dimensional Ordered Set/Pair (the order of the elements matters).
 * 
 * @author jcfgonc@gmail.com
 *
 * @param <T>
 */
public class OrderedPair<T> implements Serializable {
	private static final long serialVersionUID = -4793919736026599241L;
	private T leftElement;
	private T rightElement;

	public OrderedPair(OrderedPair<T> other) {
		super();
		this.leftElement = other.leftElement;
		this.rightElement = other.rightElement;
	}

	public OrderedPair(T leftElement, T rightElement) {
		super();
		this.leftElement = leftElement;
		this.rightElement = rightElement;
	}

	public boolean containsAnyElement(Collection<T> elements) {
		for (T element : elements) {
			if (leftElement.equals(element) || rightElement.equals(element))
				return true;
		}
		return false;
	}

	public boolean containsElement(T element) {
		if (leftElement.equals(element) || rightElement.equals(element))
			return true;
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		@SuppressWarnings("unchecked")
		OrderedPair<T> other = (OrderedPair<T>) obj;
		int ohc = other.hashCode();
		int thc = this.hashCode();
		if (ohc != thc)
			return false;

		if (leftElement == null) {
			if (other.leftElement != null)
				return false;
		}
		if (rightElement == null) {
			if (other.rightElement != null)
				return false;
		}

		// reversed analogy is the same
		if (leftElement.equals(other.rightElement) && rightElement.equals(other.leftElement))
			return true;

		if (!leftElement.equals(other.leftElement))
			return false;
		if (!rightElement.equals(other.rightElement))
			return false;
		return true;
	}

	public List<T> getElements() {
		ArrayList<T> al = new ArrayList<>(2);
		al.add(leftElement);
		al.add(rightElement);
		return al;
	}

	public T getElement(int i) {
		switch (i) {
		case 0: {
			return leftElement;
		}
		case 1: {

			return rightElement;
		}
		default:
			throw new IndexOutOfBoundsException("Index range is {0, 1}. Requested: " + i);
		}
	}

	public T getLeftElement() {
		return leftElement;
	}

	public T getOpposingElement(T element) {
		if (leftElement.equals(element))
			return rightElement;
		if (rightElement.equals(element))
			return leftElement;
		return null;
	}

	public T getRightElement() {
		return rightElement;
	}

	@Override
	public int hashCode() {
		return leftElement.hashCode() ^ rightElement.hashCode();
	}

	public boolean isSelfMapping() {
		return leftElement.equals(rightElement);
	}

	public void setElements(T leftElement, T rightElement) {
		this.leftElement = leftElement;
		this.rightElement = rightElement;
	}

	public void setLeftElement(T leftElement) {
		this.leftElement = leftElement;
	}

	public void setRightElement(T rightElement) {
		this.rightElement = rightElement;
	}

	@Override
	public String toString() {
		return leftElement + "|" + rightElement;
	}
}
