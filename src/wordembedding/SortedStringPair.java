package wordembedding;

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
public class SortedStringPair implements Serializable {
	private static final long serialVersionUID = -4793919736026599241L;
	private String lowerElement;
	private String higherElement;

	public SortedStringPair(SortedStringPair other) {
		super();
		this.lowerElement = other.lowerElement;
		this.higherElement = other.higherElement;
	}

	public SortedStringPair(String word_a, String word_b) {
		super();
		String lower = (word_a.compareTo(word_b) <= 0) ? word_a : word_b;
		String higher = (word_a.compareTo(word_b) >= 0) ? word_a : word_b;

		this.lowerElement = lower;
		this.higherElement = higher;
	}

	public boolean containsAnyElement(Collection<String> elements) {
		for (String element : elements) {
			if (lowerElement.equals(element) || higherElement.equals(element))
				return true;
		}
		return false;
	}

	public boolean containsElement(String element) {
		if (lowerElement.equals(element) || higherElement.equals(element))
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

		SortedStringPair other = (SortedStringPair) obj;
		int ohc = other.hashCode();
		int thc = this.hashCode();
		if (ohc != thc)
			return false;

		if (lowerElement == null) {
			if (other.lowerElement != null)
				return false;
		}
		if (higherElement == null) {
			if (other.higherElement != null)
				return false;
		}

		if (!lowerElement.equals(other.lowerElement))
			return false;
		if (!higherElement.equals(other.higherElement))
			return false;
		return true;
	}

	public List<String> getElements() {
		ArrayList<String> al = new ArrayList<>(2);
		al.add(lowerElement);
		al.add(higherElement);
		return al;
	}

	public String getElement(int i) {
		switch (i) {
		case 0: {
			return lowerElement;
		}
		case 1: {

			return higherElement;
		}
		default:
			throw new IndexOutOfBoundsException("Index range is {0, 1}. Requested: " + i);
		}
	}

	public String getLowerElement() {
		return lowerElement;
	}

	public String getOpposingElement(String element) {
		if (lowerElement.equals(element))
			return higherElement;
		if (higherElement.equals(element))
			return lowerElement;
		return null;
	}

	public String getHigherElement() {
		return higherElement;
	}

	@Override
	public int hashCode() {
		return lowerElement.hashCode() ^ (higherElement.hashCode());
	}

	public boolean isSelfMapping() {
		return lowerElement.equals(higherElement);
	}

	@Override
	public String toString() {
		return lowerElement + "|" + higherElement;
	}
}
