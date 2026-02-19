package wordembedding;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import structures.Ticker;
import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class WordEmbeddingReadWrite {

	public static ListWordEmbedding readCSV(String filename, boolean skipFirstLine) throws IOException {
		System.out.print("loading " + filename + " ...");
		Ticker t = new Ticker();
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
		System.out.println("done. Took " + t.getElapsedTime() + " seconds.");
		return wv;
	}

	public static ListWordEmbedding readAutoDetect(String filename, boolean skipFirstLine) throws IOException {
		if (filename.endsWith(".zip"))
			return readZippedCSV(filename, skipFirstLine);
		else
			return readCSV(filename, skipFirstLine);
	}

	public static ListWordEmbedding readZippedCSV(String filename, boolean skipFirstLine) throws IOException {
		System.out.print("loading " + filename + " ...");
		Ticker t = new Ticker();
		ListWordEmbedding wv = new ListWordEmbedding();

		final ZipFile zipFile = new ZipFile(filename);
		// assume that the zip file only has one entry: the text file of word embeddings
		final ZipEntry firstEntry = zipFile.entries().nextElement();
		if (!firstEntry.isDirectory()) {
			InputStream input = zipFile.getInputStream(firstEntry);
			NonblockingBufferedReader br = new NonblockingBufferedReader(input);
			String line;
		//	int row_counter = 0;
			while ((line = br.readLine()) != null) {
				if (skipFirstLine) {
					skipFirstLine = false;
					continue;
				}
				String[] tokens = VariousUtils.fastSplitWhiteSpace(line);
				wv.addWordVector(tokens);
			//	row_counter++;
			//	System.out.printf("read %d rows\n", row_counter);
			}
			// no more data to read
			br.close();
			System.out.println("done. Took " + t.getElapsedTime() + " seconds.");
		}
		zipFile.close();
		return wv;
	}
}
