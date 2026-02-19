package wordembedding;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import structures.StringDoublePair;

public class ListWordEmbedding {
	private HashMap<String, DoubleArrayList> vectors;

	public ListWordEmbedding() {
		vectors = new HashMap<String, DoubleArrayList>();
	}

	public DoubleArrayList addWordVector(String word, DoubleArrayList vec) {
		return vectors.put(word, vec);
	}

	public void addWordVector(String[] tokens) {
		String word = tokens[0];
		DoubleArrayList vec = new DoubleArrayList(tokens.length - 1);
		for (int i = 1; i < tokens.length; i++) {
			String tok = tokens[i];
			double d = Double.parseDouble(tok);
			vec.add(d);
		}
		addWordVector(word, vec);
	}

	public boolean containsWord(String word) {
		return vectors.containsKey(word);
	}

	public DoubleArrayList getVector(String word) {
		if (!vectors.containsKey(word)) {
			throw new RuntimeException("invalid word: " + word);
		}
		return vectors.get(word);
	}

	/**
	 * get cosine similarity, from https://stackoverflow.com/a/22913525
	 * 
	 * @param word0
	 * @param word1
	 * @return
	 */
	public double getCosineSimilarity(DoubleArrayList vec0, DoubleArrayList vec1) {
		double dotProduct = 0.0;
		double sumsq0 = 0.0;
		double sumsq1 = 0.0;
		for (int i = 0; i < vec0.size(); i++) {
			dotProduct += vec0.getDouble(i) * vec1.getDouble(i);
			sumsq0 += Math.pow(vec0.getDouble(i), 2);
			sumsq1 += Math.pow(vec1.getDouble(i), 2);
		}
		return dotProduct / (Math.sqrt(sumsq0) * Math.sqrt(sumsq1));
	}

	/**
	 * get cosine similarity, from https://stackoverflow.com/a/22913525
	 * 
	 * @param word0
	 * @param word1
	 * @return
	 */
	public double getCosineSimilarity(String word0, String word1) {
		DoubleArrayList vec0 = getVector(word0);
		DoubleArrayList vec1 = getVector(word1);

		double dotProduct = 0.0;
		double sumsq0 = 0.0;
		double sumsq1 = 0.0;
		for (int i = 0; i < vec0.size(); i++) {
			dotProduct += vec0.getDouble(i) * vec1.getDouble(i);
			sumsq0 += Math.pow(vec0.getDouble(i), 2);
			sumsq1 += Math.pow(vec1.getDouble(i), 2);
		}
		return dotProduct / (Math.sqrt(sumsq0) * Math.sqrt(sumsq1));
	}

	public double getEuclideanDistance(String word0, String word1) {
		DoubleArrayList vec0 = getVector(word0);
		DoubleArrayList vec1 = getVector(word1);
		double sum = 0;
		for (int i = 0; i < vec0.size(); i++) {
			double v0 = vec0.getDouble(i);
			double v1 = vec1.getDouble(i);
			double dif = v0 - v1;
			double difSquared = dif * dif;
			sum += difSquared;
		}
		double sqr = Math.sqrt(sum);
		return sqr;
	}

	public double getDotProduct(String word0, String word1) {
		DoubleArrayList vec0 = getVector(word0);
		DoubleArrayList vec1 = getVector(word1);
		double sum = 0;
		for (int i = 0; i < vec0.size(); i++) {
			double v0 = vec0.getDouble(i);
			double v1 = vec1.getDouble(i);
			sum += v0 * v1;
		}
		return sum;
	}

	public Set<String> getWords() {
		return vectors.keySet();
	}

	public List<StringDoublePair> getNearbyEuclideanWords(String refWord, int count) {
		ArrayList<StringDoublePair> pairs = new ArrayList<StringDoublePair>();
		for (String word : vectors.keySet()) {
			if (word.equals(refWord))
				continue;
			double r = getEuclideanDistance(word, refWord);
			pairs.add(new StringDoublePair(word, r));
		}
		sortPairs(pairs, true);
		return pairs.subList(0, count - 1);
	}

	public List<StringDoublePair> getNearbyCosineWords(String refWord, int count) {
		ArrayList<StringDoublePair> pairs = new ArrayList<StringDoublePair>();
		for (String word : vectors.keySet()) {
			if (word.equals(refWord))
				continue;
			double r = getCosineSimilarity(word, refWord);
			pairs.add(new StringDoublePair(word, r));
		}
		sortPairs(pairs, false);
		return pairs.subList(0, count - 1);
	}

	public List<StringDoublePair> getNearbyDotProductWords(String refWord, int count) {
		ArrayList<StringDoublePair> pairs = new ArrayList<StringDoublePair>();
		for (String word : vectors.keySet()) {
			if (word.equals(refWord))
				continue;
			double r = getDotProduct(word, refWord);
			pairs.add(new StringDoublePair(word, r));
		}
		sortPairs(pairs, false);
		return pairs.subList(0, count - 1);
	}

	private void sortPairs(ArrayList<StringDoublePair> pairs, boolean ascending) {
		pairs.sort(new Comparator<StringDoublePair>() {
			@Override
			public int compare(StringDoublePair o1, StringDoublePair o2) {
				if (ascending)
					return Double.compare(o1.getDouble(), o2.getDouble());
				else
					return Double.compare(o2.getDouble(), o1.getDouble());
			}
		});
	}

	@Override
	public String toString() {
		DoubleArrayList v0 = vectors.values().iterator().next();
		return vectors.size() + " words with " + v0.size() + " dimensions";
	}

	public void saveBinary(Path file) throws IOException {

		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file), 1 << 20))) {

			// Write map size
			out.writeInt(vectors.size());

			// Write entries
			for (Entry<String, DoubleArrayList> e : vectors.entrySet()) {
				String key = e.getKey();
				if (key == null) {
					throw new IllegalStateException("Null key encountered");
				}
				writeString(out, key);

				DoubleArrayList dal = e.getValue();

				int len = dal.size();
				out.writeInt(len);

				double[] data = dal.elements(); // backing array (may be larger than size())
				for (int i = 0; i < len; i++) {
					out.writeDouble(data[i]);
				}
			}
		}
	}

	public static ListWordEmbedding loadBinary(String filename) throws IOException {
		System.out.println("loading word embedding file " + filename);
		long t0 = System.currentTimeMillis();
		ListWordEmbedding obj = new ListWordEmbedding();

		try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(Path.of(filename)), 1 << 20))) {

			int mapSize = readNonNegativeInt(in, "mapSize");
			HashMap<String, DoubleArrayList> map = new HashMap<>(capacityFor(mapSize));

			for (int i = 0; i < mapSize; i++) {
				String key = readString(in);
				int len = readNonNegativeInt(in, "vectorLength");

				double[] arr = new double[len];
				for (int j = 0; j < len; j++) {
					arr[j] = in.readDouble();
				}

				// Zero-copy list wrapping the array (no defensive copy).
				DoubleArrayList vec = DoubleArrayList.wrap(arr);
				map.put(key, vec);
			}

			obj.vectors = map;
		} catch (EOFException eof) {
			throw new IOException("Truncated/corrupt file: " + filename, eof);
		}

		long t1 = System.currentTimeMillis();
		double dt = (double) (t1 - t0) / 1000.0;
		System.out.printf("read binary file %s, took %f seconds.\n", filename, dt);
		return obj;
	}

	private static void writeString(DataOutputStream out, String s) throws IOException {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = readNonNegativeInt(in, "stringLength");
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static int readNonNegativeInt(DataInputStream in, String label) throws IOException {
		int v = in.readInt();
		if (v < 0)
			throw new IOException("Negative " + label + ": " + v);
		return v;
	}

	private static int capacityFor(int size) {
		// Ensure capacity for given size with default load factor 0.75
		return (int) (size / 0.75f) + 1;
	}

}
