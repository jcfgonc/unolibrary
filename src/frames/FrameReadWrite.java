package frames;

import java.io.BufferedWriter;
import java.io.File;
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
	private static final String FRAME_SIMILARITY_HEADER = "i:edgePairs	f:SSsum	f:SSmean	f:SSstandardDeviation	f:SSmin	f:SSmax";

	public static void writePatternFramesCSV(Collection<SemanticFrame> frames, String framesPath) throws IOException {
		// order is
		// n:time n:relationTypes n:relationTypesStd n:cycles n:patternEdges n:patternVertices n:matches s:query s:pattern
		File file = new File(framesPath);
		FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8);
		BufferedWriter bw = new BufferedWriter(fw);
		// write header
		bw.write("i:relationTypes\tf:relationTypesStd\tf:edgesPerRelationTypes\ti:cycles\ti:patternEdges\ti:patternVertices\tf:matches\tg:query");
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

		// if later is still set to -1 it will not be used
		int relationTypesColumn = -1;
		int relationTypesStdColumn = -1;
		int edgesPerRelationTypesColumn = -1;
		int cyclesColumn = -1;
		int patternEdgesColumn = -1;
		int patternVerticesColumn = -1;
		int matchesColumn = -1;
		int queryColumn = -1;
		int patternColumn = -1;
		int edgePairsColumn = -1;
		int semanticSimilaritySumColumn = -1;
		int semanticSimilarityMeanColumn = -1;
		int semanticSimilarityStdDevColumn = -1;
		int semanticSimilarityMinimumColumn = -1;
		int semanticSimilarityMaximumColumn = -1;

		while ((line = br.readLine()) != null) {
			if (line.isBlank())
				continue;
			line = line.trim();
			String[] cells = VariousUtils.fastSplitWhiteSpace(line);

			if (!readFirstLine) {
				//
				// store available columns
				for (int i = 0; i < cells.length; i++) {
					String columnName = cells[i];

					switch (columnName) {
					case "i:relationTypes":
						relationTypesColumn = i;
						break;
					case "f:relationTypesStd":
						relationTypesStdColumn = i;
						break;
					case "f:edgesPerRelationTypes":
						edgesPerRelationTypesColumn = i;
						break;
					case "i:cycles":
						cyclesColumn = i;
						break;
					case "i:patternEdges":
						patternEdgesColumn = i;
						break;
					case "i:patternVertices":
						patternVerticesColumn = i;
						break;
					case "f:matches":
						matchesColumn = i;
						break;
					case "g:query":
						queryColumn = i;
						break;
					case "s:pattern":
						patternColumn = i;
						break;

					case "i:edgePairs":
						edgePairsColumn = i;
						break;
					case "f:SSsum":
						semanticSimilaritySumColumn = i;
						break;
					case "f:SSmean":
						semanticSimilarityMeanColumn = i;
						break;
					case "f:SSstandardDeviation":
						semanticSimilarityStdDevColumn = i;
						break;
					case "f:SSmin":
						semanticSimilarityMinimumColumn = i;
						break;
					case "f:SSmax":
						semanticSimilarityMaximumColumn = i;
						break;
					}
				}
				readFirstLine = true;
				continue;
			}

			SemanticFrame sf = new SemanticFrame();
			// store frame data
			for (int i = 0; i < cells.length; i++) {
				String cell = cells[i];
				if (i == relationTypesColumn) {
					sf.setRelationTypes(Integer.parseInt(cell));
					continue;
				} else if (i == relationTypesStdColumn) {
					sf.setRelationTypesStd(Double.parseDouble(cell));
					continue;
				} else if (i == edgesPerRelationTypesColumn) {
					sf.setEdgesPerRelationTypes(Double.parseDouble(cell));
					continue;
				} else if (i == cyclesColumn) {
					sf.setCycles(Integer.parseInt(cell));
					continue;
				} else if (i == patternEdgesColumn) {
					sf.setNumberOfEdges(Integer.parseInt(cell));
					continue;
				} else if (i == patternVerticesColumn) {
					sf.setNumberOfVertices(Integer.parseInt(cell));
					continue;
				} else if (i == matchesColumn) {
					sf.setMatches(Double.parseDouble(cell));
					continue;
				} else if (i == queryColumn) {
					sf.setQuery(cell);
					continue;
					// .setFrame(cell); //same as above
				} else if (i == patternColumn) {
					sf.setOriginalGraph(cell);
					continue;

					// semantic similarity stuff added later
				} else if (i == edgePairsColumn) {
					sf.setSemanticSimilarityNumberEdgePairs(Integer.parseInt(cell));
					continue;
				} else if (i == semanticSimilaritySumColumn) {
					sf.setSemanticSimilaritySum(Double.parseDouble(cell));
					continue;
				} else if (i == semanticSimilarityMeanColumn) {
					sf.setSemanticSimilarityMean(Double.parseDouble(cell));
					continue;
				} else if (i == semanticSimilarityStdDevColumn) {
					sf.setSemanticSimilarityStdDev(Double.parseDouble(cell));
					continue;
				} else if (i == semanticSimilarityMinimumColumn) {
					sf.setSemanticSimilarityMin(Double.parseDouble(cell));
					continue;
				} else if (i == semanticSimilarityMaximumColumn) {
					sf.setSemanticSimilarityMax(Double.parseDouble(cell));
					continue;
				}
			}

			frames.add(sf);
		}
		br.close();

		System.out.printf("loaded %d frames from %s\n", frames.size(), filename);
		System.out.println("loading took " + ticker.getTimeDeltaLastCall() + " s");

		return frames;
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
