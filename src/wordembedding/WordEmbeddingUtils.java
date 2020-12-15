package wordembedding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import frames.FrameReadWrite;
import frames.SemanticFrame;
import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import structures.MapOfList;
import structures.StringDoublePair;
import structures.UnorderedPair;
import utils.VariousUtils;

@SuppressWarnings("unused")
public class WordEmbeddingUtils {
	private static final String WORD_PAIR_SCORES_HEADER = "s:relation_a\ts:relation_b\tf:similarity";

	public static void main(String[] a) throws IOException, InterruptedException {

		String frames_filename = "..\\PatternMiner\\results\\resultsV22.csv";
		String synonyms_filename = "C:\\Desktop\\github\\my source code\\PatternMiner\\results\\synonyms.txt";
		String wordembedding_filename = "D:\\\\Temp\\\\ontologies\\\\word emb\\\\ConceptNet Numberbatch 19.08\\\\numberbatch-en.txt";
		String frameSimilarityFilename = "..\\PatternMiner\\results\\patterns_semantic_similarityV22.tsv";
		calculateAndSaveFrameSimilarity(frames_filename, wordembedding_filename, synonyms_filename, frameSimilarityFilename);

//		interactiveConsole(synonyms_filename, wordembedding_filename);
	}

	private static void interactiveConsole(String synonyms_filename, String wordembedding_filename) throws IOException {
		ListWordEmbedding we = WordEmbeddingReadWrite.readCSV(wordembedding_filename, true);
		MapOfList<String, String> synonyms = readSynonymWordList(synonyms_filename, we);
		Scanner s = new Scanner(System.in);

		// this is to test the list with manual data refresh and show the effect on the semantic similarity
		while (Math.random() > -1) {
			scoreWordPairs(we, synonyms);
			s.nextLine();
			synonyms = readSynonymWordList(synonyms_filename, we);
		}

		// this is an interactive console to help with word embedding
		while (true) {
			String line = s.nextLine();
			String[] args = line.split("\\s+");
			try {
				if (line.startsWith("!q"))
					break;
				if (line.startsWith("!e")) {
					List<StringDoublePair> nw = we.getNearbyEuclideanWords(args[1], Integer.parseInt(args[2]));
					printPairs(nw);
				} else if (line.startsWith("!c")) {
					List<StringDoublePair> nw = we.getNearbyCosineWords(args[1], Integer.parseInt(args[2]));
					printPairs(nw);
				} else if (line.startsWith("!d")) {
					List<StringDoublePair> nw = we.getNearbyDotProductWords(args[1], Integer.parseInt(args[2]));
					printPairs(nw);
				} else if (line.startsWith("!s")) {
					System.out.println(getCosineSimilaritySynonyms(args[1], args[2], we, synonyms));
				} else if (args[0].endsWith("*")) {
					String prefix = args[0].substring(0, args[0].indexOf('*'));
					List<String> words = getPrefixedWords(prefix, we);
					printWords(words);
				} else {
					String[] words = line.split("\\s+");
					String word0 = words[0];
					String word1 = words[1];
					double cs = we.getCosineSimilarity(word0, word1);
					double ed = we.getEuclideanDistance(word0, word1);
					System.out.printf("%f\t%f\n", cs, ed);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		s.close();
	}

	private static void calculateAndSaveFrameSimilarity(String frames_filename, String wordembedding_filename, String synonyms_filename,
			String frameSimilarityFilename) throws IOException, InterruptedException {
		ArrayList<SemanticFrame> frames = FrameReadWrite.readPatternFrames(frames_filename);
		ListWordEmbedding we = WordEmbeddingReadWrite.readCSV(wordembedding_filename, true);
		MapOfList<String, String> synonyms = readSynonymWordList(synonyms_filename, we);
		Object2DoubleOpenHashMap<UnorderedPair<String>> wps = scoreWordPairs(we, synonyms);
		ArrayList<DescriptiveStatistics> frameSimilarityStatistics = calculateFrameWithinSimilarity(frames, wps);
		FrameReadWrite.saveFrameSimilarityStatistics(frameSimilarityStatistics, frameSimilarityFilename);
	}

	public static ArrayList<DescriptiveStatistics> calculateFrameWithinSimilarity(ArrayList<SemanticFrame> frames,
			Object2DoubleOpenHashMap<UnorderedPair<String>> wps) throws IOException, InterruptedException {
		ArrayList<DescriptiveStatistics> frameSimilarityStatistics = new ArrayList<DescriptiveStatistics>();
		for (SemanticFrame frame : frames) {
			StringGraph graph = frame.getGraph();
			DescriptiveStatistics ds = calculateGraphWithinSimilarity(wps, graph);
			frameSimilarityStatistics.add(ds);
		}
		return frameSimilarityStatistics;
	}

	public static DescriptiveStatistics calculateGraphWithinSimilarity(Object2DoubleOpenHashMap<UnorderedPair<String>> wps, StringGraph graph) {
		double[] sim = calculateEdgeSemanticSimilarity(graph, wps);
		DescriptiveStatistics ds = new DescriptiveStatistics(sim);
		return ds;
	}

	/**
	 * calculates semantic similarities (based on word embedding) between pairs of connected edges in the given frame/graph. Uses the pair scores
	 * stored in the given wps argument.
	 * 
	 * @param frame
	 * @param wps
	 * @return
	 */
	public static double[] calculateEdgeSemanticSimilarity(StringGraph graph, Object2DoubleOpenHashMap<UnorderedPair<String>> wps) {
		// requires at least two edges
		if (graph.numberOfEdges() < 2) {
			return new double[0];
		}

		DoubleArrayList similarities = new DoubleArrayList();
		ObjectOpenHashSet<UnorderedPair<StringEdge>> visitedEdgePairs = new ObjectOpenHashSet<UnorderedPair<StringEdge>>();

		for (StringEdge curEdge : graph.edgeSet()) {
			HashSet<StringEdge> connectedEdges = graph.edgesOf(curEdge);
			for (StringEdge connectedEdge : connectedEdges) {
				UnorderedPair<StringEdge> curEdgePair = new UnorderedPair<StringEdge>(curEdge, connectedEdge);
				if (visitedEdgePairs.contains(curEdgePair))
					continue;
				visitedEdgePairs.add(curEdgePair);
				String rel0 = curEdge.getLabel();
				String rel1 = connectedEdge.getLabel();
				double sim = 0;
				if (rel0.equals(rel1))
					sim = 1;
				else {
					UnorderedPair<String> relationPair = new UnorderedPair<String>(rel0, rel1);
					if (wps.containsKey(relationPair)) {
						sim = wps.getDouble(relationPair);
					} else {
						System.err.println("ERROR: score undefined for the pair " + relationPair);
					}
				}
				// System.out.printf("%s %s %f\n", rel0, rel1, sim);

				similarities.add(sim);
			}
		}

		return similarities.toDoubleArray();
	}

	public static void saveWordPairScores(Object2DoubleOpenHashMap<UnorderedPair<String>> scores, String filename) throws IOException {
		File file = new File(filename);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		// write header
		bw.write(WORD_PAIR_SCORES_HEADER);
		bw.newLine();
		for (UnorderedPair<String> pair : scores.keySet()) {
			bw.write(pair.getLeft());
			bw.write('\t');
			bw.write(pair.getRight());
			bw.write('\t');
			double d = scores.getDouble(pair);
			bw.write(Double.toString(d));
			bw.newLine();
		}
		bw.close();
	}

	public static Object2DoubleOpenHashMap<UnorderedPair<String>> readWordPairScores(String filename) throws IOException {
		Object2DoubleOpenHashMap<UnorderedPair<String>> scores = new Object2DoubleOpenHashMap<UnorderedPair<String>>();
		File file = new File(filename);
		BufferedReader br = null;
		try {
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			boolean readFirstLine = false;
			while ((line = br.readLine()) != null) {
				if (line.isBlank())
					continue;
				if (!readFirstLine) {
					if (!line.equals(WORD_PAIR_SCORES_HEADER)) {
						throw new RuntimeException("first line must be of the form:" + WORD_PAIR_SCORES_HEADER);
					}
					readFirstLine = true;
					continue;
				}
				String[] cells = VariousUtils.fastSplitWhiteSpace(line);
				String word0 = cells[0];
				String word1 = cells[1];
				double score = Double.parseDouble(cells[2]);
				UnorderedPair<String> pair = new UnorderedPair<String>(word0, word1);
				scores.put(pair, score);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			br.close();
		}
		return scores;
	}

	/**
	 * calculates the semantic similarity between all the pairs of synonyms
	 * 
	 * @param we
	 * @param synonyms
	 * @return
	 */
	public static Object2DoubleOpenHashMap<UnorderedPair<String>> scoreWordPairs(ListWordEmbedding we, MapOfList<String, String> synonyms) {
		Object2DoubleOpenHashMap<UnorderedPair<String>> wordPairScore = new Object2DoubleOpenHashMap<UnorderedPair<String>>();
		// iterate through all the words
		ArrayList<String> wordList = new ArrayList<String>(synonyms.keySet());
		for (int i = 0; i < wordList.size() - 1; i++) {
			String word0 = wordList.get(i);
			for (int j = i + 1; j < wordList.size(); j++) {
				String word1 = wordList.get(j);
				// word0 and word1 are relations/concepts (present in the first column of the synonyms file)
				// calculate the cosine similarity between the two words using their synonyms
				double score = getCosineSimilaritySynonyms(word0, word1, we, synonyms);
				UnorderedPair<String> wordPair = new UnorderedPair<String>(word0, word1);
				wordPairScore.put(wordPair, score);
			}
		}
		return wordPairScore;
	}

	public static List<String> getPrefixedWords(String prefix, ListWordEmbedding we) {
		ArrayList<String> words = new ArrayList<String>();
		for (String word : we.getWords()) {
			if (word.startsWith(prefix)) {
				words.add(word);
			}
		}
		return words;
	}

	public static void printPairs(List<StringDoublePair> nw) {
		if (nw.isEmpty()) {
			System.out.println("[]");
			return;
		}
		for (StringDoublePair pair : nw) {
			System.out.printf("%s\t%f\n", pair.getString(), pair.getDouble());
		}
	}

	public static void printWords(List<String> words) {
		if (words.isEmpty()) {
			System.out.println("[]");
			return;
		}
		for (String w : words) {
			System.out.printf("%s\t", w);
		}
		System.out.printf("\n");
	}

	public static MapOfList<String, String> readSynonymWordList(String filename, ListWordEmbedding we) throws IOException {
		MapOfList<String, String> synonyms = new MapOfList<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			String[] tokens = VariousUtils.fastSplitWhiteSpace(line);
			String word = tokens[0];
			HashSet<String> existing = new HashSet<String>(tokens.length * 3);
			for (int i = 1; i < tokens.length; i++) {
				String syn = tokens[i];
				if (we.containsWord(syn)) {
					if (!existing.contains(syn)) {
						synonyms.add(word, syn);
						existing.add(syn);
					}
				}
			}
		}
		br.close();
		return synonyms;
	}

	public static double getCosineSimilaritySynonyms(String word0, String word1, ListWordEmbedding we, MapOfList<String, String> synonyms) {
		List<String> synonyms0 = synonyms.get(word0);
		List<String> synonyms1 = synonyms.get(word1);

		// find the synonym pair which maximizes the similarity
		HashSet<UnorderedPair<String>> checkedPairs = new HashSet<UnorderedPair<String>>(1024);
		double highestCS = -Double.MAX_VALUE;
//		 UnorderedPair<String> highestCS_pair = null;
		for (String syn0 : synonyms0) {
			for (String syn1 : synonyms1) {

				UnorderedPair<String> currentPair = new UnorderedPair<String>(syn0, syn1);
				if (checkedPairs.contains(currentPair))
					continue;
				checkedPairs.add(currentPair);

				double cs = we.getCosineSimilarity(syn0, syn1);
				if (cs > highestCS) {
					// highestCS_pair = currentPair;
					highestCS = cs;
				}
				// System.out.printf("%s\t%s\t%f\n", syn0, syn1, cs);
			}
		}
		// System.out.printf("%s\t%s\t%f\n", new UnorderedPair<String>(word0, word1), highestCS_pair, highestCS);
		// System.out.println(highestCS_pair);

		// return we.getCosineSimilarity(synonyms0.get(0), synonyms1.get(0));
		return highestCS;
	}

	public static void getOveralSimilarity(StringGraph frame) {

	}

}
