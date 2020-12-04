package wordembedding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.StringDoublePair;

public class ListWordEmbedding {
	private HashSet<String> words;
	private Object2IntOpenHashMap<String> wordToId;
	private ArrayList<DoubleArrayList> vectors;

	public ListWordEmbedding() {
		words = new HashSet<String>();
		wordToId = new Object2IntOpenHashMap<String>();
		vectors = new ArrayList<DoubleArrayList>();
	}

	public void addWordVector(String word, DoubleArrayList vec) {
		int index = words.size();
		boolean alreadyExists = !words.add(word);
		if (alreadyExists) {
			index = wordToId.getInt(word);
			vectors.set(index, vec);
		} else {
			wordToId.put(word, index);
			vectors.add(vec);
		}
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
		return words.contains(word);
	}

	public DoubleArrayList getVector(String word) {
		if (!wordToId.containsKey(word)) {
			throw new RuntimeException("invalid word: " + word);
		}
		int index = wordToId.getInt(word);
		return vectors.get(index);
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

	public ArrayList<String> getWords() {
		return new ArrayList<String>(words);
	}

	public List<StringDoublePair> getNearbyEuclideanWords(String refWord, int count) {
		ArrayList<StringDoublePair> pairs = new ArrayList<StringDoublePair>();
		for (String word : words) {
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
		for (String word : words) {
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
		for (String word : words) {
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
		DoubleArrayList v0 = vectors.get(0);
		return vectors.size() + " words with " + v0.size() + " dimensions";
	}
}
