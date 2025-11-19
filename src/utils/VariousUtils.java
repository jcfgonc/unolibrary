package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.FastMath;

import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import structures.ObjectCounter;

public class VariousUtils {
	public static final Set<StringEdge> unmodifiableEmptySet = Collections.unmodifiableSet(new HashSet<StringEdge>(0));

	public static int countWords(String concept) {
		concept = concept.replaceAll("[\t ]+", " ").trim();
		int spaces = VariousUtils.countCharOccurences(concept, ' ');
		return spaces + 1;
	}

	public static int countCharOccurences(String str, char c) {
		int count = 0;
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (str.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}

	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	/**
	 * reads each row of the given text file as an element of the returned ArrayList
	 * 
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<String> readFileRows(String filename) throws FileNotFoundException {
		boolean illegalFilename = true;
		ArrayList<String> rows = new ArrayList<String>(1 << 10);
		try {
			if (filename != null) {

				File f = new File(filename);
				if (f.exists() && !f.isDirectory()) {

					NonblockingBufferedReader br = new NonblockingBufferedReader(filename);
					String line;
					while ((line = br.readLine()) != null) {
						if (line.isBlank())
							continue;
						rows.add(line);
					}
					br.close();
					illegalFilename = false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (illegalFilename) {
			throw new FileNotFoundException(filename);
		}
		return rows;
	}

	public static String integerToString(int n) {
		char[] buf = new char[(int) Math.floor(Math.log(25 * (n + 1)) / Math.log(26))];
		for (int i = buf.length - 1; i >= 0; i--) {
			n--;
			buf[i] = (char) ('A' + n % 26);
			n /= 26;
		}
		return new String(buf);
	}

	public static void waitForEnter() {
		waitForEnter(null, (Object[]) null);
	}

	public static void waitForEnter(String message, Object... args) {
		Console c = System.console();
		if (c != null) {
			// printf-like arguments
			if (message != null)
				c.format(message, args);
			c.format("\nPress ENTER to proceed.\n");
			c.readLine();
		} else {
			System.err.println("system returned a null console, unable to wait for enter keypress");
		}
	}

	public static String generateCurrentDateAndTimeStamp() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String filename = dateFormat.format(date);
		return filename;
	}

	/**
	 * from https://stackoverflow.com/questions/3585053/how-to-check-if-a-string-contains-only-ascii
	 * 
	 * @param v
	 * @return
	 */
	public static boolean isPureAscii(String v) {
		return StandardCharsets.US_ASCII.newEncoder().canEncode(v);
		// or "ISO-8859-1" for ISO Latin 1
		// or StandardCharsets.US_ASCII with JDK1.7+
	}

	public String readStringFromURL(URL u) throws IOException {
		// URL u = new URL("http://www.example.com/");
		try (InputStream in = u.openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * adapt this code to whatever you need, from https://stackoverflow.com/a/30443276
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String parseFileMT(String filePath) throws IOException, InterruptedException {
		ExecutorService executor = Executors.newWorkStealingPool();
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		List<String> parsed = Collections.synchronizedList(new ArrayList<>());
		try {
			String line;
			while ((line = br.readLine()) != null) {
				final String l = line;
				executor.submit(() -> {
					if (l.charAt(0) == '!') {
						parsed.add(l);
					}
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.MINUTES);

		String result = parsed.stream().collect(Collectors.joining("\n"));
		return result;
	}

	public static <T> T getRandomElementFromCollection(Collection<T> collection, RandomGenerator random) {
		int size = collection.size();
		if (size == 0) { // shortcut in case the collection only has one element
			return collection.iterator().next();
		}

		int index = random.nextInt(size);
		T obj = getElementFromCollection(collection, index);
		return obj;
	}

	public static <T> T getElementFromCollection(Collection<T> collection, int index) {
		if (collection instanceof List<?>) {
			List<T> asList = (List<T>) collection;
			T obj = asList.get(index);
			return obj;
		}

		Iterator<T> iter = collection.iterator();
		for (int j = 0; j < index; j++) {
			iter.next();
		}
		return iter.next();

	}

	public static double getRandomDoublePow(RandomGenerator random, double pow) {
		return FastMath.pow(random.nextDouble(), pow);
	}

	/**
	 * returns the intersection of the given sets
	 *
	 * @param vertexSet0
	 * @param vertexSet1
	 * @return
	 */
	public static <T> HashSet<T> intersection(Collection<T> vertexSet0, Collection<T> vertexSet1) {
		HashSet<T> intersection = new HashSet<T>(2 * (Math.max(vertexSet0.size(), vertexSet1.size())));
		for (T vertex0 : vertexSet0) {
			if (vertexSet1.contains(vertex0)) {
				intersection.add(vertex0);
			}
		}
		return intersection;
	}

	public static <T> Set<T> union(Collection<T> set0, Collection<T> set1) {
		HashSet<T> set = new HashSet<T>(2 * (set0.size() + set1.size()));
		set.addAll(set0);
		set.addAll(set1);
		return set;
	}

	/**
	 * returns true if both sets intersect
	 *
	 * @param vertexSet0
	 * @param vertexSet1
	 * @return
	 */
	public static <T> boolean intersects(Collection<T> vertexSet0, Collection<T> vertexSet1) {
		for (T vertex0 : vertexSet0) {
			if (vertexSet1.contains(vertex0)) {
				return true;
			}
		}
		return false;
	}

	public static <T> Set<T> randomSubSet(Collection<T> set, int numberOfElements, RandomGenerator random) {
		return randomSubSetOld(set, numberOfElements, random);
		// if (set instanceof RandomAccess) {
		// return randomSampleArrayList((ArrayList<T>) set, numberOfElements,
		// random);
		// } else {
		// return randomSampleSet(set, numberOfElements, random);
		// }
	}

	// my old version
	private static <T> Set<T> randomSubSetOld(Collection<T> set, int numberOfElements, RandomGenerator random) {
		ArrayList<T> list = new ArrayList<T>(set); // N
		shuffleArrayList(list, random); // N
		Set<T> randomSet = new HashSet<T>();
		for (int i = 0; i < numberOfElements; i++) { // N
			randomSet.add(list.get(i));
		}
		// ~= N + N + N ~= 3N
		return randomSet;
	}

	// taken from https://eyalsch.wordpress.com/2010/04/01/random-sample/
	// Floyd's Algorithm
	public static <T> HashSet<T> randomSampleArrayList(ArrayList<T> array, int size, RandomGenerator random) {
		HashSet<T> set = new HashSet<T>(size);
		int n = array.size();
		for (int i = n - size; i < n; i++) {
			int pos = random.nextInt(i + 1);
			T item = array.get(pos);
			if (set.contains(item))
				set.add(array.get(i));
			else
				set.add(item);
		}
		return set;
	}

	// taken from https://eyalsch.wordpress.com/2010/04/01/random-sample/
	// Full Scan
	public static <T> HashSet<T> randomSampleSet(Collection<T> collection, int size, RandomGenerator random) {
		HashSet<T> set = new HashSet<T>(size);
		int visited = 0;
		Iterator<T> it = collection.iterator();
		while (size > 0) {
			T item = it.next();
			if (random.nextDouble() < ((double) size) / (collection.size() - visited)) {
				set.add(item);
				size--;
			}
			visited++;
		}
		return set;
	}

	// Implementing Fisher-Yates shuffle, it shuffles the array in-place
	public static <T> void shuffleArrayList(ArrayList<T> array, RandomGenerator random) {
		for (int i = array.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			T element = array.get(j);
			array.set(j, array.get(i));
			array.set(i, element);
		}
	}

	/**
	 * Better ArrayList alternative to Java's Arrays.asList(array).
	 * 
	 * @param array
	 * @return the array as an ArrayList
	 */
	public static <T> ArrayList<T> arrayToArrayList(final T[] array) {
		ArrayList<T> list = new ArrayList<T>(array.length);
		for (final T s : array) {
			list.add(s);
		}
		return list;
	}

	/**
	 * returns the subtraction of the set {@code b} from the set {@code a} as a new set<br>
	 * := a-b <br>
	 * or in another words, the elements of {@code a} NOT IN {@code b}
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static <E> Set<E> subtract(Set<E> a, Set<E> b) {
		if (b.isEmpty()) {
			return a;
		}

		if (a.isEmpty()) {
			return a;
		}

		HashSet<E> set = new HashSet<E>(2 * a.size());
		for (E e : a) {
			if (!b.contains(e)) {
				set.add(e);
			}
		}

		// cache optimization
		if (set.size() == a.size())
			return a;

		return set;
	}

	/**
	 * returns the subtraction of the set {@code b} from the set {@code a} as a new set<br>
	 * := a-b <br>
	 * or in another words, the elements of {@code a} NOT IN {@code b}
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static Set<StringEdge> subtract(Set<StringEdge> a, Set<StringEdge> b, boolean processBlends) {
		processBlends = false;
		if (b.isEmpty()) {
			return a;
		}

		if (a.isEmpty()) {
			return a;
		}

		if (processBlends) {
			return subtractBlendedEdges(a, b);
		} else {
			HashSet<StringEdge> set = new HashSet<StringEdge>(2 * a.size());
			for (StringEdge e : a) {
				if (!b.contains(e)) {
					set.add(e);
				}
			}
			return set;
		}
	}

	private static Set<StringEdge> subtractBlendedEdges(Set<StringEdge> a, Set<StringEdge> b) {
		HashSet<StringEdge> processedB = new HashSet<StringEdge>(b.size() * 2);
		for (StringEdge edge : b) {
			if (edge.containsBlendedConcept()) {
				ArrayList<StringEdge> split = edge.splitBlend();
				processedB.addAll(split);
			} else {
				processedB.add(edge);
			}
		}
		HashSet<StringEdge> set = new HashSet<StringEdge>(2 * a.size());
		for (StringEdge edgeInA : a) {
			if (edgeInA.containsBlendedConcept()) {
				// TODO test this
				System.err.println("TODO test this: search code for 36a86b0f187e4045");
				// in reality this should not happen, otherwise it implies that the input space
				// has blended concepts
				ArrayList<StringEdge> split = edgeInA.splitBlend();
				boolean disjoint = Collections.disjoint(split, processedB);
				if (disjoint) {
					set.add(edgeInA);
				}
			} else {
				if (!processedB.contains(edgeInA)) {
					set.add(edgeInA);
				}
			}
		}
		return set;
	}

	@SafeVarargs
	public static <E> Set<E> mergeSets(Set<E>... sets) {
		// allocate a large set for the merge
		int nElements = 0;
		for (Set<E> set : sets) {
			nElements += set.size();
		}
		HashSet<E> merged = new HashSet<E>(2 * nElements);
		// do the merge
		for (Set<E> set : sets) {
			merged.addAll(set);
		}
		return merged;
	}

	/**
	 * adapted from https://codereview.stackexchange.com/a/111257 to support multiple separators
	 * 
	 * @param text
	 * @return
	 */
	public static String[] fastSplit(final String text, CharOpenHashSet separators) {
		if (text == null) {
			throw new IllegalArgumentException("the text to split should not be null");
		}

		final List<String> result = new ArrayList<String>(256); // 4(reference size)*256<=4k (page size)

		final int len = text.length();
		int tokenStart = 0;
		boolean prevCharIsSeparator = true; // "preceding char is separator" flag

		for (int pos = 0; pos < len; ++pos) {
			char c = text.charAt(pos);

			// if (c == separator0 || c == separator1) {
			if (separators.contains(c)) {
				if (!prevCharIsSeparator) {
					result.add(text.substring(tokenStart, pos));
					prevCharIsSeparator = true;
				}
				tokenStart = pos + 1;
			} else {
				prevCharIsSeparator = false;
			}
		}

		if (tokenStart < len) {
			result.add(text.substring(tokenStart));
		}

		String[] arr = new String[result.size()];
		result.toArray(arr);

		return arr;
	}

	/**
	 * adapted from https://codereview.stackexchange.com/a/111257 to support multiple separators
	 * 
	 * @param text
	 * @return
	 */
	public static String[] fastSplit(final String text, final char separator) {
		if (text == null) {
			throw new IllegalArgumentException("the text to split should not be null");
		}

		final List<String> result = new ArrayList<String>(256); // 4(reference size)*256<=4k (page size)

		final int len = text.length();
		int tokenStart = 0;
		boolean prevCharIsSeparator = true; // "preceding char is separator" flag

		for (int pos = 0; pos < len; ++pos) {
			char c = text.charAt(pos);

			// if (c == separator0 || c == separator1) {
			if (c == separator) {
				if (!prevCharIsSeparator) {
					result.add(text.substring(tokenStart, pos));
					prevCharIsSeparator = true;
				}
				tokenStart = pos + 1;
			} else {
				prevCharIsSeparator = false;
			}
		}

		if (tokenStart < len) {
			result.add(text.substring(tokenStart));
		}

		String[] arr = new String[result.size()];
		result.toArray(arr);

		return arr;
	}

	public static String[] fastSplitWhiteSpace(final String text) {
		if (text == null) {
			throw new IllegalArgumentException("the text to split should not be null");
		}

		final List<String> result = new ArrayList<String>(256); // 4(reference size)*256<=4k (page size)

		final int len = text.length();
		int tokenStart = 0;
		boolean prevCharIsSeparator = true; // "preceding char is separator" flag

		for (int pos = 0; pos < len; ++pos) {
			char c = text.charAt(pos);

			if (c == ' ' || c == '\t') {
				if (!prevCharIsSeparator) {
					result.add(text.substring(tokenStart, pos));
					prevCharIsSeparator = true;
				}
				tokenStart = pos + 1;
			} else {
				prevCharIsSeparator = false;
			}
		}

		if (tokenStart < len) {
			result.add(text.substring(tokenStart));
		}

		String[] arr = new String[result.size()];
		result.toArray(arr);

		return arr;
	}

	public static String[] fastSplit(final String text, final String separators_str) {
		CharOpenHashSet separators = new CharOpenHashSet();
		for (int i = 0; i < separators_str.length(); i++) {
			separators.add(separators_str.charAt(i));
		}
		return VariousUtils.fastSplit(text, separators);
	}

	/**
	 * returns an ascending sequence of integers from the range min (inclusive) to max (exclusive)
	 * 
	 * @param minInclusive
	 * @param maxExclusive
	 * @return
	 */
	public static int[] intRange(int minInclusive, int maxExclusive) {
		int n = maxExclusive - minInclusive;
		int[] array = new int[n];
		int val = minInclusive;
		for (int i = 0; i < n; i++) {
			array[i] = val;
			val++;
		}
		return array;
	}

	/**
	 * returns the given collection as a shuffled ArrayList
	 * 
	 * @param <T>
	 * @param col
	 * @param random
	 * @return
	 */
	public static <T> ArrayList<T> asShuffledArray(Collection<T> col, Random random) {
		ArrayList<T> shuffledArr = new ArrayList<>(col);
		Collections.shuffle(shuffledArr, random);
		return shuffledArr;
	}

	public static void printArray(double[] arr) {
		if (arr == null) {
			System.out.println("null");
		} else {
			if (arr.length == 0) {
				return;
			} else {
				for (int i = 0; i < arr.length - 1; i++) {
					System.out.print(Double.toString(arr[i]));
					System.out.print('\t');
				}
				System.out.println(Double.toString(arr[arr.length - 1]));
			}
		}
	}

	public static <T> HashSet<T> calculateRemovedElements(HashSet<T> oldSet, HashSet<T> newSet) {
		// what's in old which ain't in new
		HashSet<T> removed = new HashSet<>();
		for (T element : oldSet) {
			if (!newSet.contains(element)) {
				removed.add(element);
			}
		}
		return removed;
	}

	public static <T> HashSet<T> calculateAddedElements(HashSet<T> oldSet, HashSet<T> newSet) {
		// what's in new which ain't in old
		HashSet<T> added = new HashSet<>();
		for (T element : newSet) {
			if (!oldSet.contains(element)) {
				added.add(element);
			}
		}
		return added;
	}

	/**
	 * Returns the (approx) logarithm to the base 2 of the given BigInteger. This function calculates base 2 log because finding the number of occupied bits is trivial.
	 * 
	 * @param val
	 * @return
	 */
	public static double log2(BigInteger val) {
		// from https://stackoverflow.com/a/9125512 by Mark Jeronimus
		// ---
		// Get the minimum number of bits necessary to hold this value.
		int n = val.bitLength();

		// Calculate the double-precision fraction of this number; as if the
		// binary point was left of the most significant '1' bit.
		// (Get the most significant 53 bits and divide by 2^53)
		long mask = 1L << 52; // mantissa is 53 bits (including hidden bit)
		long mantissa = 0;
		int j = 0;
		for (int i = 1; i < 54; i++) {
			j = n - i;
			if (j < 0)
				break;

			if (val.testBit(j))
				mantissa |= mask;
			mask >>>= 1;
		}
		// Round up if next bit is 1.
		if (j > 0 && val.testBit(j - 1))
			mantissa++;

		double f = mantissa / (double) (1L << 52);

		// Add the logarithm to the number of bits, and subtract 1 because the
		// number of bits is always higher than necessary for a number
		// (ie. log2(val)<n for every val).
		return (n - 1 + Math.log(f) * 1.44269504088896340735992468100189213742664595415298D);
		// Magic number converts from base e to base 2 before adding. For other
		// bases, correct the result, NOT this number!
	}

	public static String appendSuffixToFilename(String prefix, String suffix) {
		int lastDotIndex = prefix.lastIndexOf('.');
		String s = prefix.substring(0, lastDotIndex) + suffix + prefix.substring(lastDotIndex);
		return s;
	}

	public static int doubleCompareTo(double d1, double d2, double epsilon) {
		if (d1 < d2 - epsilon)
			return -1;
		if (d1 > d2 + epsilon)
			return 1;
		return 0;
	}

	public static int getNextAvailableFileId(String folder, String fileTemplate, String extension) {
		final String ext;
		if (!extension.startsWith(".")) { // because of the below regex
			ext = "." + extension;
		} else {
			ext = extension;
		}

		String regex = fileTemplate + "[\\d]+" + ext;
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		// make sure folder is accessible
		File f = new File(folder);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Can not use " + folder + " as a folder because it exists as a file");
		}
		if (!f.exists()) {
			f.mkdir();
		}
		// get file list
		File[] list = f.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				String name = file.getName();
				Matcher matcher = pattern.matcher(name);
				if (matcher.matches()) {
					return true;
				}
				return false;
			}
		});

		Arrays.sort(list, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				String[] name1 = fastSplit(o1.getName(), '_');
				String[] name2 = fastSplit(o2.getName(), '_');
				String t1 = name1[1];
				String t2 = name2[1];
				String sub1 = t1.substring(0, t1.indexOf(ext));
				String sub2 = t2.substring(0, t2.indexOf(ext));
				int num1 = Integer.parseInt(sub1);
				int num2 = Integer.parseInt(sub2);
				return Integer.compare(num1, num2);
			}
		});

		if (list.length == 0) {
			return 0;
		} else {
			// get next available integer
			String last = list[list.length - 1].getName();
			String[] name1 = fastSplit(last, '_');
			String t1 = name1[1];
			String sub1 = t1.substring(0, t1.indexOf(ext));
			int num1 = Integer.parseInt(sub1);
			return num1 + 1;
		}
	}

	public static double getUsedMemoryMB() {
		double MB = 1024 * 1024;
		// get Runtime instance
		Runtime instance = Runtime.getRuntime();
		// used memory
		double used = (double) (instance.totalMemory() - instance.freeMemory()) / MB;
		return used;
	}

	/**
	 * decodes number of threads from the given text as a number
	 * 
	 * @param text
	 * @return
	 */
	public static int parseNumberThreads(String text) {
		// ]0...1[ = percentage of total Executing Units in the system
		// <=0 = all EUs
		// [1...+oo[ = specific whole number of EUs

		int maxThreadNum = OSTools.getNumberOfLogicalProcessors();

		double asDouble = Double.valueOf(text).doubleValue();
		if (asDouble <= 1e-20) {
			return maxThreadNum;
		}
		if (asDouble >= 1) {
			return (int) asDouble;
		}
		double x = asDouble * (double) maxThreadNum;
		return (int) x;
	}

	public static <E> ArrayList<E> sublistOf(Collection<E> set, int size) {
		ArrayList<E> list = new ArrayList<E>(size);
		Iterator<E> iterator = set.iterator();
		int counter = 0;
		while (iterator.hasNext()) {
			if (counter >= size)
				break;
			E next = iterator.next();
			list.add(next);
			counter++;
		}
		return list;
	}

	/**
	 * removes a bunch of elements from the given deque, up to a maximum of n or the size of the deque, whatever is smaller
	 * 
	 * @param <E>
	 * @param deque
	 * @param n
	 * @return
	 */
	public static <E> ArrayList<E> removeMultipleElementsFromDeque(Deque<E> deque, int n) {
		ArrayList<E> list = new ArrayList<E>(n);
		if (deque.isEmpty())
			throw new NoSuchElementException();
		n = Math.min(n, deque.size());
		for (int i = 0; i < n; i++) {
			list.add(deque.remove());
		}
		return list;
	}

	public static String getCurrentWorkingDirectory() {
		String cwd = Path.of("").toAbsolutePath().toString();
		return cwd;
	}

	public static ObjectCounter<String> countEdgeTargetsOf(StringGraph graph, String relation) {
		ObjectCounter<String> targetCounter = new ObjectCounter<String>();
		for (StringEdge edge : graph.edgeSet(relation)) {
			targetCounter.addObject(edge.getTarget());
		}
		return targetCounter;
	}

	public static void writeFileRows(String filename, Collection<String> strings) throws IOException {
		FileWriter fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw, 1 << 16);
		for (String str : strings) {
			bw.write(str);
			bw.newLine();
		}
		bw.close();
		fw.close();
	}

	/**
	 * returns a sublist starting at start and ending at end or at its tail, whichever is smaller
	 * 
	 * @param <T>
	 * @param list
	 * @param start
	 * @param end
	 * @return
	 */
	public static <T> List<T> sublistMin(List<T> list, int start, int end) {
		int size = list.size();
		if (end >= size) {
			end = size - 1;
		}
		return list.subList(start, end);
	}

	public static <T> ArrayList<T> removeBlockFromDeque(Deque<T> deque, int nElements) {
		ArrayList<T> extractedElements = new ArrayList<T>();
		if (nElements > deque.size()) {
			nElements = deque.size();
		}
		for (int i = 0; i < nElements; i++) {
			extractedElements.add(deque.remove());
		}
		return extractedElements;
	}

	/**
	 * Removes diacritics (~= accents) from a string. The case will not be altered.
	 * 
	 * For instance, 'à' will be replaced by 'a'.
	 * 
	 * Calls Apache Commons Lang3 StringUtils.stripAccents(String input)
	 * 
	 * @param text
	 * @return
	 */
	public static String stripAccents(String text) {
		return StringUtils.stripAccents(text);
	}

	public static boolean checkIfFileExists(String filepath) {
		if (filepath != null && !filepath.isEmpty()) {
			File f = new File(filepath);
			if (f.exists() && !f.isDirectory()) {
				return true;
			}
		}
		return false;
	}

	public static int ORedIndexOf(String main, String str0, String str1) {
		int i0 = main.indexOf(str0);
		if (i0 != -1)
			return i0;
		int i1 = main.indexOf(str1);
		return i1;
	}

	public static <T> void printList(List<T> list) {
		Iterator<T> iterator = list.iterator();
		while (iterator.hasNext()) {
			T next = iterator.next();
			System.out.println(next);
		}
	}

	public static HashMap<String, String> readTwoColumnFile(String filePath) {
		HashMap<String, String> templates = new HashMap<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				assert tokens.length == 2;
				String relation = tokens[0];
				String template = tokens[1];
				templates.put(relation, template);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return templates;
	}

	public static HashMap<String, String> readTwoColumnFile(String filePath, String column_separator) {
		HashMap<String, String> templates = new HashMap<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(column_separator);
				assert tokens.length == 2;
				String relation = tokens[0];
				String template = tokens[1];
				templates.put(relation, template);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return templates;
	}

	public static boolean whiteSpaceCheck(String str) {
		boolean startsWithSpace = !str.isEmpty() && Character.isWhitespace(str.charAt(0));
		boolean endsWithSpace = !str.isEmpty() && Character.isWhitespace(str.charAt(str.length() - 1));

		if (startsWithSpace || endsWithSpace) {
			return true;
		}
		return false;
	}

}
