package frames;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import graph.StringGraph;
import structures.Ticker;
import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class FrameReadWrite {
	private static final String FRAME_PATTERN_HEADER = "i:relationTypes	f:relationTypesStd	f:edgesPerRelationTypes	i:cycles	i:patternEdges	i:patternVertices	f:matches	g:query	s:pattern";
	private static final String FRAME_SIMILARITY_HEADER = "i:edgePairs	f:SSsum	f:SSmean	f:SSstandardDeviation	f:SSmin	f:SSmax";

	public static void writePatternFramesCSV(Collection<SemanticFrame> frames, String framesPath) throws IOException {
		// order is
		// n:time n:relationTypes n:relationTypesStd n:cycles n:patternEdges n:patternVertices n:matches s:query s:pattern
		File file = new File(framesPath);
		FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8);
		BufferedWriter bw = new BufferedWriter(fw);
		// write header
		bw.write(FRAME_PATTERN_HEADER);
		bw.newLine();
		for (SemanticFrame frame : frames) {
			StringGraph graph = frame.getFrame();
			bw.write(Integer.toString(frame.getRelationTypes()));
			bw.write('\t');
			bw.write(Double.toString(frame.getRelationTypesStd()));
			bw.write('\t');
			bw.write(Double.toString(frame.getEdgesPerRelationTypes()));
			bw.write('\t');
			bw.write(Integer.toString(frame.getCycles()));
			bw.write('\t');
			bw.write(Integer.toString(graph.numberOfEdges()));
			bw.write('\t');
			bw.write(Integer.toString(graph.numberOfVertices()));
			bw.write('\t');
			bw.write(Double.toString(frame.getMatches()));
			bw.write('\t');
			bw.write(frame.getFrameString());
			bw.write('\t');
			bw.write(frame.getOriginalGraph());
			bw.write('\t');
			bw.newLine();
		}
		bw.close();
		fw.close();
	}

	public static ArrayList<SemanticFrame> readPatternFrames(String filename) throws IOException, InterruptedException {
		Ticker ticker = new Ticker();
		NonblockingBufferedReader br = new NonblockingBufferedReader(filename);
		ArrayList<SemanticFrame> frames = new ArrayList<SemanticFrame>();

		String line;
		boolean readFirstLine = false;
		while ((line = br.readLine()) != null) {
			if (line.isBlank())
				continue;
			line = line.trim();
			if (!readFirstLine) {
				if (!line.equals(FRAME_PATTERN_HEADER)) {
					throw new RuntimeException("first line must be:" + FRAME_PATTERN_HEADER);
				}
				readFirstLine = true;
				continue;
			}

			String[] cells = VariousUtils.fastSplitWhiteSpace(line);

			SemanticFrame sf = new SemanticFrame();
			// store frame data
			sf.setRelationTypes(Integer.parseInt(cells[0]));
			sf.setRelationTypesStd(Double.parseDouble(cells[1]));
			sf.setEdgesPerRelationTypes(Double.parseDouble(cells[2]));
			sf.setCycles(Integer.parseInt(cells[3]));

			sf.setMatches(Double.parseDouble(cells[6]));
			sf.setFrame(cells[7]);
			// sf.setPatternGraph(GraphReadWrite.readCSVFromString(cells[6])); // graph is built from the query
			sf.setOriginalGraph(cells[8]);

			frames.add(sf);
		}
		br.close();

		String filenameSemanticStuff = VariousUtils.appendSuffixToFilename(filename, "_semantic_similarity");
		if (new File(filenameSemanticStuff).isFile()) {
			FrameReadWrite.updatePatternFrameSimilarity(frames, filenameSemanticStuff);
		}

		System.out.printf("loaded %d frames from %s\n", frames.size(), filename);
		System.out.println("loading took " + ticker.getTimeDeltaLastCall() + " s");

		return frames;
	}

	/**
	 * updates the given list of frames with semantic similarity data from the specified filename
	 * 
	 * @param frames
	 * @param similarity_filename
	 * @throws IOException
	 */
	public static void updatePatternFrameSimilarity(ArrayList<SemanticFrame> frames, String similarity_filename) throws IOException {
		FileReader fr = new FileReader(similarity_filename);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(fr);
		String line;
		int counter = 0;
		boolean readFirstLine = false;
		while ((line = br.readLine()) != null) {
			if (line.isBlank())
				continue;
			line = line.trim();
			if (!readFirstLine) {
				if (!line.equals(FRAME_SIMILARITY_HEADER)) {
					throw new RuntimeException("first line must be:" + FRAME_SIMILARITY_HEADER);
				}
				readFirstLine = true;
				continue;
			}

			SemanticFrame frame = frames.get(counter);
			String[] cells = VariousUtils.fastSplitWhiteSpace(line);

			// 0----------1-----2------3-------------------4-----5
			// #edgePairs SSsum SSmean SSstandardDeviation SSmin SSmax;
			frame.setSemanticSimilarityNumberEdgePairs(Integer.parseInt(cells[0]));
			frame.setSemanticSimilaritySum(Double.parseDouble(cells[1]));
			frame.setSemanticSimilarityMean(Double.parseDouble(cells[2]));
			frame.setSemanticSimilarityStdDev(Double.parseDouble(cells[3]));
			frame.setSemanticSimilarityMin(Double.parseDouble(cells[4]));
			frame.setSemanticSimilarityMax(Double.parseDouble(cells[5]));

			counter++;
		}
		br.close();

		System.out.printf("loaded %d frames from %s\n", frames.size(), similarity_filename);
	}

	public static void saveFrameSimilarityStatistics(ArrayList<DescriptiveStatistics> frameSimilarityStatistics, String filename) throws IOException {
		File file = new File(filename);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file), 1 << 16);
		// write header
		bw.write(FRAME_SIMILARITY_HEADER);
		bw.newLine();
		for (DescriptiveStatistics ds : frameSimilarityStatistics) {
			long edgePairs = ds.getN();
			double sum = ds.getSum();
			double mean = ds.getMean();
			double sd = ds.getStandardDeviation();
			double min = ds.getMin();
			double max = ds.getMax();
			String text = edgePairs + "\t" + sum + "\t" + mean + "\t" + sd + "\t" + min + "\t" + max;
			bw.write(text);
			bw.newLine();
		}
		bw.close();
	}

}
