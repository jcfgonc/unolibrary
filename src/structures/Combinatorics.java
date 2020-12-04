package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class Combinatorics {

	/**
	 * receives something such as [[(1,23), (1,21)], [(3,28)], [(8,23), (8,21)]] and returns something such as [[(1,23), (3,28), (8,21)],
	 * [(1,21)], (3,28), [(8,23)]]
	 *
	 * @param bagsOfElements
	 * @return
	 */
	public static <T> ArrayList<ArrayList<T>> kPartialPermutation(ArrayList<ArrayList<T>> bagsOfElements) {
		final boolean debug = false;
		ArrayList<ArrayList<T>> results = new ArrayList<>();

		if (bagsOfElements.isEmpty())
			return results;

		int nBags = bagsOfElements.size();

		int permutations = 1;
		int[] stepLength = new int[nBags];
		for (int i = 0; i < nBags; i++) {
			ArrayList<T> elementsBag = bagsOfElements.get(i);
			int nElementsBag = elementsBag.size();
			stepLength[i] = permutations;
			permutations *= nElementsBag;
		}

		for (int combi = 0; combi < permutations; combi++) { // 24
			ArrayList<T> combination = new ArrayList<>();
			for (int bagi = 0; bagi < nBags; bagi++) { // 3
				ArrayList<T> elements = bagsOfElements.get(bagi);
				int elementIndex = (combi / stepLength[bagi]) % elements.size();
				if (debug)
					System.out.printf("%d\t", elementIndex);
				T element = elements.get(elementIndex);
				combination.add(element);
				if (debug)
					System.out.printf("%s\t", element);
			}
			if (debug)
				System.out.println();
			results.add(combination);
		}
		return results;
	}

	/**
	 * Returns a collection of sets where each set is composed of the combination of the given elements (in the originalSet). I.e. {1,2,3}
	 * -> (1,2,3) (1,3,2) (2,1,3). Recursive version?
	 *
	 * @param originalSet
	 * @return
	 */
	public static <T> ArrayList<ArrayList<T>> powerSetvr(Collection<T> originalSet) {
		ArrayList<ArrayList<T>> sets = new ArrayList<ArrayList<T>>();
		if (originalSet.isEmpty()) {
			sets.add(new ArrayList<T>());
			return sets;
		}
		ArrayList<T> list = new ArrayList<T>(originalSet);
		T head = list.get(0);
		ArrayList<T> rest = new ArrayList<T>(list.subList(1, list.size()));
		for (ArrayList<T> set : powerSetvr(rest)) {
			ArrayList<T> newSet = new ArrayList<T>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	/**
	 * Returns a collection of sets where each set is composed of the combination of the given elements (in the originalSet). I.e. {1,2,3}
	 * -> (1,2,3) (1,3,2) (2,1,3). Iterative version?
	 *
	 * @param originalSet
	 * @return
	 */
	public static <T> ArrayList<ArrayList<T>> powerSetvi(Collection<T> originalSet) {
		ArrayList<ArrayList<T>> ps = new ArrayList<ArrayList<T>>();
		ps.add(new ArrayList<T>()); // add the empty set

		// for every item in the original list
		for (T item : originalSet) {
			ArrayList<ArrayList<T>> newPs = new ArrayList<ArrayList<T>>();

			for (ArrayList<T> subset : ps) {
				// copy all of the current powerset's subsets
				newPs.add(subset);

				// plus the subsets appended with the current item
				ArrayList<T> newSubset = new ArrayList<T>(subset);
				newSubset.add(item);
				newPs.add(newSubset);
			}

			// powerset is now powerset of list.subList(0, list.indexOf(item)+1)
			ps = newPs;
		}
		return ps;
	}

	public static Iterator<int[]> combinations(int n, int k) {
		return CombinatoricsUtils.combinationsIterator(n, k);
	}

}
