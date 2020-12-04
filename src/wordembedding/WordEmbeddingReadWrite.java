package wordembedding;

import java.io.IOException;

import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class WordEmbeddingReadWrite {

	public static ListWordEmbedding readCSV(String filename, boolean skipFirstLine) throws IOException {
		// Ticker t = new Ticker();
		ListWordEmbedding wv = new ListWordEmbedding();
		NonblockingBufferedReader br = new NonblockingBufferedReader(filename);
		String line;
		while ((line = br.readLine()) != null) {
			if (skipFirstLine) {
				skipFirstLine = false;
				continue;
			}
			String[] tokens = VariousUtils.fastSplitWhiteSpace(line);
			wv.addWordVector(tokens);
		}
		// no more data to read
		br.close();
		// System.out.println("took " + t.getElapsedTime() + " seconds to read " + wv.toString());
		return wv;
	}

	public static void main(String[] args) throws IOException {
		String filename = "D:\\Temp\\ontologies\\word emb\\GloVe - Global Vectors for Word Representation\\glove.6B.300d.txt";
		while (true)
			readCSV(filename, false);
	}
}
