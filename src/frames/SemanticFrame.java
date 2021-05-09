package frames;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import graph.GraphReadWrite;
import graph.StringGraph;

public class SemanticFrame {

	/**
	 * (equivalent to frame_str but as a StringGraph) frameGraph is the subgraph from the input spaces which has variables as vertices
	 */
	private StringGraph frameGraph;
	private double matches;
	private int cycles;
	private double relationTypesStd;
	private int relationTypes;
	/**
	 * originalGraph is the subgraph from the input spaces which has concepts as vertices
	 */
	private String originalGraph;
	/**
	 * frame_str is the subgraph from the input spaces which has variables as vertices
	 */
	private String frame_str;
	private double ssmin;
	private double ssmean;
	private double ssmax;
	private int ssEdgePairs;
	private double ssSum;
	private double ssStdDev;
	private double edgesPerRelationTypes;

	public SemanticFrame() {
	}

	public int getCycles() {
		return cycles;
	}

	public double getEdgesPerRelationTypes() {
		return edgesPerRelationTypes;
	}

	/**
	 * returns the graph which has variables (capital words) as vertices
	 * 
	 * @return
	 */
	public StringGraph getFrame() {
		return frameGraph;
	}

	/**
	 * frame is the subgraph from the input spaces which has variables as vertices
	 * 
	 * @return
	 */
	public String getFrameString() {
		return frame_str;
	}

	public double getMatches() {
		return matches;
	}

	/**
	 * originalGraph is the subgraph from the input spaces which has concepts as vertices
	 * 
	 * @return
	 */
	public String getOriginalGraph() {
		return originalGraph;
	}

	public int getRelationTypes() {
		return relationTypes;
	}

	public double getRelationTypesStd() {
		return relationTypesStd;
	}

	public double getSemanticSimilarityMax() {
		return this.ssmax;
	}

//	public void setPatternGraph(StringGraph graph) {
//		this.patternGraph = graph;
//	}

	public double getSemanticSimilarityMean() {
		return this.ssmean;
	}

	public double getSemanticSimilarityMin() {
		return this.ssmin;
	}

	public int getSemanticSimilarityNumberEdgePairs() {
		return this.ssEdgePairs;
	}

	public double getSemanticSimilarityStdDev() {
		return this.ssStdDev;
	}

	public double getSemanticSimilaritySum() {
		return this.ssSum;
	}

	public void setCycles(int c) {
		this.cycles = c;
	}

	public void setEdgesPerRelationTypes(double val) {
		this.edgesPerRelationTypes=val;
	}

	/**
	 * frame is the subgraph from the input spaces which has variables as vertices
	 * 
	 * @param string
	 * @throws NoSuchFileException
	 * @throws IOException
	 */
	public void setFrame(String string) throws NoSuchFileException, IOException {
		this.frame_str = string;
		this.frameGraph = GraphReadWrite.readCSVFromString(string);
	}

	public void setMatches(double matches) {
		this.matches = matches;
	}

	/**
	 * originalGraph is the subgraph from the input spaces which has concepts as vertices
	 * 
	 * @param string
	 */
	public void setOriginalGraph(String string) {
		this.originalGraph = string;
	}

	public void setRelationTypes(int rt) {
		this.relationTypes = rt;
	}

	public void setRelationTypesStd(double rts) {
		this.relationTypesStd = rts;
	}

	public void setSemanticSimilarityMax(double val) {
		this.ssmax = val;
	}

	public void setSemanticSimilarityMean(double val) {
		this.ssmean = val;
	}

	public void setSemanticSimilarityMin(double val) {
		this.ssmin = val;
	}

	public void setSemanticSimilarityNumberEdgePairs(int val) {
		this.ssEdgePairs = val;
	}

	public void setSemanticSimilarityStdDev(double val) {
		this.ssStdDev = val;
	}

	public void setSemanticSimilaritySum(double val) {
		this.ssSum = val;
	}

	public String toString() {
		return "relationTypes=" + relationTypes + //
				", relationTypesStd=" + relationTypesStd + //
				", cycles=" + cycles + //
				", matches=" + matches + //
				", query=" + frame_str + //
				", original=" + originalGraph + //
				", graph=" + frameGraph + "]";
	}

}
