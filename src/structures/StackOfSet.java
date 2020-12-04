package structures;

import java.util.ArrayDeque;
import java.util.HashSet;

/**
 * A stack where each element is a set of the given type. Created for convenience.
 * 
 * @author João Gonçalves: jcfgonc@gmail.com
 *
 * @param <E>
 */
public class StackOfSet<E> {

	private ArrayDeque<HashSet<E>> aq;

	public StackOfSet() {
		this.aq = new ArrayDeque<>();
	}

	public boolean isEmpty() {
		return aq.isEmpty();
	}

	public HashSet<E> pop() {
		return aq.pop();
	}

	// do a copy to prevent mismatched pops and pushes
	public void push(HashSet<E> hs) {
		aq.push(new HashSet<>(hs));
	}

	public String toString() {
		return aq.toString();
	}
}
