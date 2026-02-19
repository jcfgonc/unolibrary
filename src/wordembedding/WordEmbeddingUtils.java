package wordembedding;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import structures.SynchronizedSeriarizableHashMap;
import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class WordEmbeddingUtils {
//	private static SynchronizedSeriarizableHashMap<SortedStringPair<String>, Double> cachedCosineSimilarities = new SynchronizedSeriarizableHashMap<>("cachedCosineSimilarities.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedWord = new SynchronizedSeriarizableHashMap<>("cachedWord.dat", 30);
	private static SynchronizedSeriarizableHashMap<SortedStringPair, Double> cachedCosineSim = new SynchronizedSeriarizableHashMap<>("cachedCosineSim.dat", 30);
	private static Object2DoubleOpenHashMap<String> word_relative_freq;
	private static ListWordEmbedding word_embedding;
	private static boolean initialized = false;

	public static void main(String[] a) throws IOException, InterruptedException {

//		getCosineSimilarity(we, "feline", "canine");
//		getCosineSimilarity(we, "door", "window");
		ArrayList<String> pairs = VariousUtils.readFileRows("D:\\Desktop\\mapeamentos.txt");
		for (String pair : pairs) {
			getCosineSimilarity(pair);
		}
		saveCaches();
	}

	public static void saveCaches() {
		try {
			cachedCosineSim.save();
			cachedWord.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initialize() throws IOException {
		// create word to relative frequency map
//		Object2LongOpenHashMap<String> word_freq = readWordFrequencyCSV("D:\\My Source Code\\Java - PhD\\UnoLibrary\\data\\Google Books Ngram word freq 9.2M\\ngram_freq.csv");
//		Object2DoubleOpenHashMap<String> word_relfreq = normalizeWordFrequencyMap(word_freq);
//		saveBinary(word_relfreq, "word_relfreq.bin");

		WordEmbeddingUtils.word_relative_freq = loadBinary_Object2DoubleOpenHashMap("word_relfreq.bin");

//		ArrayList<String> stop_words = VariousUtils.readFileRows("D:\\My Source Code\\Java - PhD\\UnoLibrary\\data\\word lists\\english stop words full.txt");

//		String word_emb_file = "D:\\My Source Code\\Java - PhD\\UnoLibrary\\data\\word vectors\\ENC3 English Common Crawl Corpus 300d 2M\\model.zip";
		// ListWordEmbedding we = WordEmbeddingReadWrite.readAutoDetect(word_emb_file, false);
		// we.saveBinary(Path.of("wordemb.bin"));
		WordEmbeddingUtils.word_embedding = ListWordEmbedding.loadBinary("wordemb.bin");
	}

	public static void saveBinary(Object2DoubleOpenHashMap<String> map, String filename) throws IOException {
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Path.of(filename)), 1 << 20))) {

			// Write map size
			int map_size = map.size();
			out.writeInt(map_size);

			// Write entries
			for (Object2DoubleMap.Entry<String> e : map.object2DoubleEntrySet()) {
				String key = e.getKey();
				if (key == null) {
					throw new IllegalStateException("Null key encountered");
				}
				writeString(out, key);
				double val = e.getDoubleValue();
				out.writeDouble(val);
			}
		}
	}

	private static void writeString(DataOutputStream out, String s) throws IOException {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	public static Object2DoubleOpenHashMap<String> loadBinary_Object2DoubleOpenHashMap(String filename) throws IOException {
		System.out.println("loading Object2DoubleOpenHashMap file " + filename);
		long t0 = System.currentTimeMillis();
		Object2DoubleOpenHashMap<String> map = null;
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(Path.of(filename)), 1 << 20))) {

			int mapSize = in.readInt();
			map = new Object2DoubleOpenHashMap<>(mapSize * 2);

			for (int i = 0; i < mapSize; i++) {
				String key = readString(in);
				double val = in.readDouble();
				map.put(key, val);
			}

		} catch (EOFException eof) {
			throw new IOException("Truncated/corrupt file: " + filename, eof);
		}

		long t1 = System.currentTimeMillis();
		double dt = (double) (t1 - t0) / 1000.0;
		System.out.printf("read binary file %s, took %f seconds.\n", filename, dt);
		return map;
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = in.readInt();
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static Object2DoubleOpenHashMap<String> normalizeWordFrequencyMap(Object2LongOpenHashMap<String> word_freq) {
		// scan for the smallest frequency
		long smallest = Long.MAX_VALUE;
		LongIterator iterator = word_freq.values().iterator();
		while (iterator.hasNext()) {
			long nextLong = iterator.nextLong();
			if (nextLong < smallest)
				smallest = nextLong;
		}
		// normalize all the frequencies using the largest
		Object2DoubleOpenHashMap<String> word_relfreq = new Object2DoubleOpenHashMap<>();
		for (String word : word_freq.keySet()) {
			long freq = word_freq.getLong(word);
			double relfreq = (double) smallest / freq;
			word_relfreq.put(word, relfreq);
		}
		return word_relfreq;
	}

	public static Object2LongOpenHashMap<String> readWordFrequencyCSV(String filename) throws FileNotFoundException, IOException {
		System.out.printf("loading word frequency csv file %s\n", filename);
		long t0 = System.currentTimeMillis();
		// we assume that the file does not have a header
		NonblockingBufferedReader br = new NonblockingBufferedReader(new FileReader(filename));
		Object2LongOpenHashMap<String> map = new Object2LongOpenHashMap<>(1 << 24);
		{
			// read each line into the list
			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = VariousUtils.fastSplit(line, ',');
				String word = tokens[0];
				long frequency = Long.parseLong(tokens[1]);
				map.put(word, frequency);
			}
		}
		long t1 = System.currentTimeMillis();
		long dt = t1 - t0;
		System.out.printf("loading done, took %f seconds\n", (double) dt / 1000);
		return map;
	}

	private static String correctEmbeddingWord(ListWordEmbedding we, String word) {
		String we_word = cachedWord.get(word);
		if (we_word == null) {
			if (we.containsWord(word)) {
				we_word = word;
			} else {
				String string1 = word.replace(' ', ':');
				String string2 = word.replace(' ', '_');
				String string3 = word.replace(' ', '-');
				if (we.containsWord(string1)) {
					we_word = string1;
				} else if (we.containsWord(string2)) {
					we_word = string2;
				} else if (we.containsWord(string3)) {
					we_word = string3;
				}
			}
		} else {
			return we_word;
		}
		if (we_word != null) {
			cachedWord.put(word, we_word);
		}
		return we_word;
	}

	private static double getCosineSimilarity(String string) {
		String[] words = VariousUtils.fastSplit(string, '|');
		return getCosineSimilarity(words[0], words[1]);
	}

	public static double getCosineSimilarity(String word_a, String word_b) {
		if (!initialized) {
			try {
				initialize();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			initialized = true;
		}

		SortedStringPair ssp = new SortedStringPair(word_a, word_b);
		Double simil = cachedCosineSim.get(ssp);
		if (simil == null) {
			String lower = correctEmbeddingWord(word_embedding, ssp.getLowerElement());
			String higher = correctEmbeddingWord(word_embedding, ssp.getHigherElement());
			DoubleArrayList lowerVector = null;
			DoubleArrayList higherVector = null;

			if (lower != null) {
				lowerVector = word_embedding.getVector(lower);
			} else { // does not exist in the word emb vector db
				// calculate SIF of composite
				lowerVector = calculateCompositeWordEmbedding(word_embedding, word_relative_freq, ssp.getLowerElement());
			}
			if (higher != null) {
				higherVector = word_embedding.getVector(higher);
			} else {
				higherVector = calculateCompositeWordEmbedding(word_embedding, word_relative_freq, ssp.getHigherElement());
			}

			simil = Double.valueOf(word_embedding.getCosineSimilarity(lowerVector, higherVector));
			cachedCosineSim.put(ssp, simil);
		}
		System.out.printf("cosine similarity\t%s\t%s\t%f\n", ssp.getLowerElement(), ssp.getHigherElement(), simil.doubleValue());
		return simil.doubleValue();
	}

	private static DoubleArrayList calculateCompositeWordEmbedding(ListWordEmbedding we, Object2DoubleOpenHashMap<String> word_relfreq, String text) {
		String[] words = VariousUtils.fastSplit(text, ' ');
		double[] accumulated_sum = null;
		for (String word : words) {
			if (word.equals("4004"))
				System.lineSeparator();
			double[] vector = we.getVector(word).toDoubleArray();
			if (nullVector(vector))
				throw new RuntimeException("word: " + word + " vector: " + vector.toString() + " is null!");
			// word frequency map might be more limited in supported words
			double weight = word_relfreq.getDouble(word);
			if (weight < Double.MIN_NORMAL) {
				weight = deduceRelativeFrequencyOfComposite(word, word_relfreq);
				// throw new RuntimeException("word: " + word + " weight: " + weight + " is null!");
			}
			if (accumulated_sum == null) {
				// must initialize here because we don't know the vector size
				accumulated_sum = new double[vector.length];
			}
			accumulated_sum = vectorMultiplyAdd(accumulated_sum, vector, weight);
		}
		accumulated_sum = normalizeVector(accumulated_sum);
		return new DoubleArrayList(accumulated_sum);
	}

	/**
	 * used in text having words not present in the word frequency table
	 * 
	 * @param word
	 * @param word_relfreq
	 * @return
	 */
	private static double deduceRelativeFrequencyOfComposite(String word, Object2DoubleOpenHashMap<String> word_relfreq) {
		String[] tokens = VariousUtils.fastSplit(word, " -:.,_");
		double mult = 1.0;
		for (String token : tokens) {
			double d = word_relfreq.getDouble(token);
			mult *= d;
		}
		return mult;
	}

	private static boolean nullVector(double[] vector) {
		for (double x : vector) {
			if (x > Double.MIN_NORMAL)
				return false;
		}
		return true;
	}

	/**
	 * Returns a new vector that is the L2-normalized (unit-length) version of the input.
	 *
	 * @param v the input vector (double[])
	 * @return a new double[] with norm 1.0
	 * @throws IllegalArgumentException if v is null, empty, contains non-finite values, or is the zero vector (norm == 0)
	 */
	public static double[] normalizeVector(double[] v) {

		double sumSquares = 0.0;
		for (double x : v) {
			if (!Double.isFinite(x)) {
				throw new IllegalArgumentException("Vector contains non-finite value: " + x);
			}
			sumSquares += x * x;
		}

		double norm = Math.sqrt(sumSquares);
		if (norm == 0.0) {
			throw new IllegalArgumentException("Cannot normalize the zero vector.");
		}

		double inv = 1.0 / norm;
		double[] out = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			out[i] = v[i] * inv;
		}
		return out;
	}

	public static double[] vectorMultiplyAdd(double[] vector_sum, double[] vector, double weight) {
		for (int i = 0; i < vector_sum.length; i++) {
			vector_sum[i] = vector[i] * weight;
		}
		return vector_sum;
	}

}
