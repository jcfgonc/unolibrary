package frames;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import graph.StringGraph;
import structures.Ticker;
import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class FrameReadWrite {

	public static void writePatternFramesCSV(Collection<SemanticFrame> frames, String framesPath) throws IOException {
		File file = new File(framesPath);
		FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8);
		BufferedWriter bw = new BufferedWriter(fw);
		// ALL IS HERE
		// write header
		bw.write("i:relationTypes" + "\t" + "f:relationTypesStd" + "\t" + "f:edgesPerRelationTypes" + "\t" + "i:cycles" + "\t" + "i:patternEdges"
				+ "\t" + "i:patternVertices" + "\t" + "i:highestDegreeVertex" + "\t" + "f:matches" + "\t" //
				+ "i:edgePairs" + "\t" + "f:SSsum" + "\t" + "f:SSmean" + "\t" + "f:SSstandardDeviation" + "\t" + "f:SSmin" + "\t" + "f:SSmax" + "\t" //
				+ "f:vrMean" + "\t" + "f:vrMin" + "\t" //
				+ "g:query" + "\t" + "s:pattern");
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
			bw.write(Integer.toString(frame.getHighestVertexDegree()));
			bw.write('\t');
			bw.write(Double.toString(frame.getMatches()));
			bw.write('\t');

			bw.write(Integer.toString(frame.getSemanticSimilarityNumberEdgePairs()));
			bw.write('\t');
			bw.write(Double.toString(frame.getSemanticSimilaritySum()));
			bw.write('\t');
			bw.write(Double.toString(frame.getSemanticSimilarityMean()));
			bw.write('\t');
			bw.write(Double.toString(frame.getSemanticSimilarityStdDev()));
			bw.write('\t');
			bw.write(Double.toString(frame.getSemanticSimilarityMin()));
			bw.write('\t');
			bw.write(Double.toString(frame.getSemanticSimilarityMax()));
			bw.write('\t');

			bw.write(Double.toString(frame.getVitalRelationsMean()));
			bw.write('\t');
			bw.write(Double.toString(frame.getVitalRelationsMin()));
			bw.write('\t');

			bw.write(frame.getFrameString());
			bw.write('\t');
			bw.write(frame.getOriginalGraph());
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
		int highestDegreeColumn=-1;
		int matchesColumn = -1;
		int edgePairsColumn = -1;
		int semanticSimilaritySumColumn = -1;
		int semanticSimilarityMeanColumn = -1;
		int semanticSimilarityStdDevColumn = -1;
		int semanticSimilarityMinimumColumn = -1;
		int semanticSimilarityMaximumColumn = -1;
		int vrMeanColumn = -1;
		int vrMinColumn = -1;
		int queryColumn = -1;
		int patternColumn = -1;

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
					case "i:highestDegreeVertex":
						highestDegreeColumn = i;
						break;						
					case "f:matches":
						matchesColumn = i;
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

					case "f:vrMean":
						vrMeanColumn = i;
						break;
					case "f:vrMin":
						vrMinColumn = i;
						break;
						
					case "g:query":
						queryColumn = i;
						break;
					case "s:pattern":
						patternColumn = i;
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
				} else if (i == highestDegreeColumn) {
					sf.setHighestVertexDegree(Integer.parseInt(cell));
					continue;
				} else if (i == matchesColumn) {
					sf.setMatches(Double.parseDouble(cell));
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

				} else if (i == vrMeanColumn) {
					sf.setVitalRelationsMean(Double.parseDouble(cell));
					continue;
				} else if (i == vrMinColumn) {
					sf.setVitalRelationsMin(Double.parseDouble(cell));
					continue;
					
				} else if (i == queryColumn) {
					sf.setQuery(cell);
					continue;
					// .setFrame(cell); //same as above
				} else if (i == patternColumn) {
					sf.setOriginalGraph(cell);
					continue;
				}

			}

			frames.add(sf);
		}
		br.close();

		System.out.printf("loaded %d frames from %s in %f s\n", frames.size(), filename, ticker.getTimeDeltaLastCall());
		return frames;
	}
}
