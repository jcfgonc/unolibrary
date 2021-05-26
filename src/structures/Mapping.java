package structures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import utils.VariousUtils;

/**
 * A mapping is a set of ConceptPair.
 * 
 * @author jcfgonc@gmail.com
 *
 * @param <T>
 */
public class Mapping<T> implements Iterable<ConceptPair<T>> {
	/**
	 * parses a file composed of lines each containing comma separated pairs of concepts, i.e.
	 * <p>
	 * a|b,c|d,e|f,...
	 * 
	 * @return
	 * 
	 * @return
	 */
	public static ArrayList<Mapping<String>> readMappingsCSV(File f) throws IOException {
		ArrayList<Mapping<String>> mappings = new ArrayList<Mapping<String>>();
		BufferedReader br = new BufferedReader(new FileReader(f), 1 << 16);
		while (br.ready()) { // break into lines
			String line = br.readLine().trim();
			// parse lines into a mapping
			Mapping<String> map = new Mapping<String>();
			// split line into N pairs
			String[] columns = VariousUtils.fastSplit(line, ',');
			for (String column : columns) { // a|b
				// split pair
				String[] concepts = VariousUtils.fastSplit(column, '|');
				if (concepts.length != 2) {
					System.err.printf("found %n concepts in pair %s, should be only 2\n", concepts.length, line);
				}
				map.add(concepts[0], concepts[1]);
			}
			mappings.add(map);
		}
		br.close();
		return mappings;
	}

	/**
	 * This reads a divago file composed of the lines in the format: <br>
	 * <i>m(alignment, horse/ear, bird/wing).</i>
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Mapping<String> readMappingsDT(File file) throws IOException {
		Mapping<String> map = new Mapping<>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()) {
			String line = br.readLine();
			if (line == null)
				break;
			line = line.trim();
			// ignore empty lines
			if (line.length() == 0)
				continue;
			// ignore commented lines
			if (line.startsWith(":-"))
				continue;
			// get text after the domain "alignment" and before the ending parentheses
			String cleaned = line.substring(line.indexOf(",") + 1, line.lastIndexOf(")"));
			StringTokenizer st = new StringTokenizer(cleaned, ",");

			String leftConcept = st.nextToken().trim();
			String rightConcept = st.nextToken().trim();

			if (leftConcept.equals(rightConcept)) {
				System.out.println("LOOP: " + line);
				continue;
			}
			ConceptPair<String> pair = new ConceptPair<String>(leftConcept, rightConcept);
			map.add(pair);
		}
		br.close();
		return map;
	}

	/**
	 * Reads a text file of mappings with each line having concepts separated by white spaces. The format is as follows: <i><br>
	 * - mapping 1 <br>
	 * concept1 concept2 <br>
	 * concept3 concept4 <br>
	 * - mapping 2 <br>
	 * concept5 concept6 <br>
	 * concept7 concept8 </i>
	 * 
	 * @param file
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static List<Mapping<String>> readMappingsTXT(File file) throws IOException {
		ArrayList<Mapping<String>> listOfMappings = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		Mapping<String> mapping = new Mapping<>();
		listOfMappings.add(mapping);
		while (br.ready()) {
			String line = br.readLine().trim();
			if (line.startsWith("-")) { // a line starting with - defines a new mapping
				mapping = new Mapping<>();
				listOfMappings.add(mapping);
				continue;
			}
			String[] tokens = VariousUtils.fastSplitWhiteSpace(line); // break using white space characters
			if (tokens.length == 2) {
				String leftConcept = tokens[0];
				String rightConcept = tokens[1];
				ConceptPair<String> pair = new ConceptPair<String>(leftConcept, rightConcept);
				mapping.add(pair);
			}
		}
		br.close();
		return listOfMappings;
	}

	/**
	 * parses a line containing comma separated pairs of concepts, i.e.
	 * <p>
	 * a|b,c|d,e|f,...
	 * 
	 * @return
	 */
	public static Mapping<String> readMappingCSVLine(String line) {
		Mapping<String> map = new Mapping<String>();
		String[] columns = VariousUtils.fastSplit(line, ',');
		for (String column : columns) { // a|b
			String[] concepts = VariousUtils.fastSplit(column, '|');
			if (concepts.length != 2) {
				System.err.printf("found %n concepts in pair %s\n", concepts.length, line);
			}
			map.add(concepts[0], concepts[1]);
		}
		return map;
	}

	public static <T> void writeMappingCSV(Mapping<T> map, File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		// go trough the pairs
		Set<ConceptPair<T>> mapping = map.getConceptPairsUnsafe();
		Iterator<ConceptPair<T>> iterator = mapping.iterator();
		while (iterator.hasNext()) {
			ConceptPair<T> pair = iterator.next();
			bw.write(pair.toString());
			// if there are more pairs, put them after a comma
			if (iterator.hasNext()) {
				bw.write(',');
			}
		}
		bw.newLine();
		bw.close();
	}

	public static <T> void writeMappingDT(Mapping<T> map, File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(":-multifile m/3.");
		bw.newLine();
		bw.newLine();

		Set<ConceptPair<T>> conceptPairs = map.getConceptPairsUnsafe();
		for (ConceptPair<T> pair : conceptPairs) {
			// m(alignment,horse,dragon).
			String leftConcept = pair.getLeftConcept().toString();
			String rightConcept = pair.getRightConcept().toString();
			String r = String.format("m(%s,%s,%s).", "alignment", leftConcept, rightConcept);
			bw.write(r);
			bw.newLine();
		}
		bw.close();
	}

	public static <T> void writeMappingsCSV(Collection<Mapping<T>> mappings, String filename) throws IOException {
		writeMappingsCSV(mappings, new File(filename));
	}

	public static <T> void writeMappingsCSV(Collection<Mapping<T>> mappings, File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (Mapping<T> mapping : mappings) {
			// go trough the pairs
			Set<ConceptPair<T>> cps = mapping.getConceptPairsUnsafe();
			Iterator<ConceptPair<T>> iterator = cps.iterator();
			while (iterator.hasNext()) {
				ConceptPair<T> pair = iterator.next();
				bw.write(pair.toString());
				// if there are more pairs, put them after a comma
				if (iterator.hasNext()) {
					bw.write(',');
				}
			}
			bw.newLine();
		}
		bw.close();
	}

	private HashMap<T, ConceptPair<T>> conceptToPair; // maps a concept to its pair
	private Set<ConceptPair<T>> mapping;
	private Set<T> leftConcepts;

	private Set<T> rightConcepts;

	private HashMap<T, T> opposingConcepts;

	public Mapping() {
		super();
		this.mapping = new HashSet<>(16, 0.333333f);
		this.conceptToPair = new HashMap<T, ConceptPair<T>>(16, 0.333333f);
		this.leftConcepts = new HashSet<>(16, 0.333333f);
		this.rightConcepts = new HashSet<>(16, 0.333333f);
		this.opposingConcepts = new HashMap<>(16, 0.333333f);
	}

	public Mapping(Collection<ConceptPair<T>> mapping_) {
		super();
		this.addAll(mapping_);
	}

	public void add(ConceptPair<T> pair) {
		T l = pair.getLeftConcept();
		T r = pair.getRightConcept();
		if (conceptToPair.containsKey(l)) {
			System.err.println("adding " + pair + " error: concept " + l + " is already in the mapping as " + conceptToPair.get(l));
			return;
		}
		if (conceptToPair.containsKey(r)) {
			System.err.println("adding " + pair + " error: concept " + r + " is already in the mapping as " + conceptToPair.get(r));
			return;
		}
		mapping.add(pair);
		conceptToPair.put(l, pair);
		conceptToPair.put(r, pair);
		leftConcepts.add(l);
		rightConcepts.add(r);
		opposingConcepts.put(l, r);
		opposingConcepts.put(r, l);
	}

	public void add(T leftConcept, T rightConcept) {
		add(new ConceptPair<T>(leftConcept, rightConcept));
	}

	public void addAll(Collection<ConceptPair<T>> pairs) {
		for (ConceptPair<T> pair : pairs) {
			add(pair);
		}
	}

	public void clear() {
		mapping.clear();
		conceptToPair.clear();
		leftConcepts.clear();
		rightConcepts.clear();
	}

	/**
	 * checks if this mapping contains a concept pair with the given concept
	 * 
	 * @param concept
	 * @return
	 */
	public boolean containsConcept(T concept) {
		return conceptToPair.containsKey(concept);
	}

	public boolean containsConceptPair(ConceptPair<T> o) {
		return mapping.contains(o);
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
		Mapping<T> other = (Mapping<T>) obj;
		if (mapping == null) {
			if (other.mapping == null)
				return true;
			else
				return false;
		} else if (mapping != null) {
			if (other.mapping == null)
				return false;
			else {
				if (other.hashCode() != this.hashCode())
					return false;
				else
					return this.mapping.equals(other.mapping);
			}
		}
		return false;

	}

	/**
	 * returns the concept pair in this mapping containing the given concept
	 * 
	 * @param concept
	 * @return
	 */
	public ConceptPair<T> getConceptPair(T concept) {
		return conceptToPair.get(concept);
	}

	/**
	 * the same as getMapping() but SAFE
	 * 
	 * @return
	 */
	public Set<ConceptPair<T>> getConceptPairs() {
		return Collections.unmodifiableSet(mapping);
	}

	/**
	 * the same as getConceptPairs() but UNSAFE
	 * 
	 * @return
	 */
	private Set<ConceptPair<T>> getConceptPairsUnsafe() {
		return mapping;
	}

	public Set<T> getLeftConcepts() {
		return Collections.unmodifiableSet(leftConcepts);
	}

	public T getOppositeConcept(T concept) {
		return opposingConcepts.get(concept);
	}

	/**
	 * returns the Nth pair in the mapping, order is defined by the internal hashset containing the pairs
	 * 
	 * @param index
	 * @return
	 */
	public ConceptPair<T> getPair(int index) {
		Iterator<ConceptPair<T>> iter = this.iterator();
		for (int i = 0; i < index; i++) {
			iter.next();
		}
		return iter.next();
	}

	/**
	 * returns a random concept pair from this mapping
	 * 
	 * @param randomGenerator
	 * @return
	 */
	public ConceptPair<T> getRandomPair(Random randomGenerator) {
		int index = randomGenerator.nextInt(size());
		return getPair(index);
	}

	public Set<T> getRightConcepts() {
		return Collections.unmodifiableSet(rightConcepts);
	}

	/**
	 * returns the number of concept pairs in this mapping
	 * 
	 * @return
	 */
	public int getSize() {
		return mapping.size();
	}

	@Override
	public int hashCode() {
		return mapping.hashCode();
	}

	public boolean isEmpty() {
		return mapping.isEmpty();
	}

	public boolean isOppositePair(T l, T r) {
		return opposingConcepts.get(l).equals(r);
	}

	public Iterator<ConceptPair<T>> iterator() {
		return mapping.iterator();
	}

	public boolean remove(ConceptPair<T> o) {
		T l = o.getLeftConcept();
		T r = o.getRightConcept();
		conceptToPair.remove(l);
		conceptToPair.remove(r);
		leftConcepts.remove(l);
		rightConcepts.remove(r);
		return mapping.remove(o);
	}

	public int size() {
		return mapping.size();
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		// go trough the pairs
		Iterator<ConceptPair<T>> iterator = getConceptPairsUnsafe().iterator();
		while (iterator.hasNext()) {
			ConceptPair<T> pair = iterator.next();
			s.append(pair.toString());
			// if there are more pairs, put them after a comma
			if (iterator.hasNext()) {
				s.append(',');
			}
		}
		return s.toString();
	}

	public void renameConcept(T old, T neww) {
		if (!containsConcept(old))
			return;
		// is old left or right?
		if (leftConcepts.contains(old)) {
			T other = getOppositeConcept(old);
			ConceptPair<T> oldCP = getConceptPair(old);
			remove(oldCP);
			// section differs here
			add(neww, other);
		} else if (rightConcepts.contains(old)) {
			T other = getOppositeConcept(old);
			ConceptPair<T> oldCP = getConceptPair(old);
			remove(oldCP);
			// section differs here
			add(other, neww);
		}
	}

}
