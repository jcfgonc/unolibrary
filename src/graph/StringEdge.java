package graph;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;

import utils.VariousUtils;

public class StringEdge implements Comparable<StringEdge>, Serializable, Cloneable {
	private static final long serialVersionUID = 8432349686429608349L;
	private String source = null;
	private String target = null;
	private String label = null;
	private int hashcode = 0;
	private boolean hashcodeCached = false;
	public static final int CSV_ORDER_SOURCE_TARGET_LABEL = 0;
	public static final int CSV_ORDER_LABEL_SOURCE_TARGET = 1;
	public static final int CSV_ORDER_SOURCE_LABEL_TARGET = 2;

	public StringEdge(String source, String target, String label) {
		this.source = source;
		this.target = target;
		this.label = label;
	}

	/**
	 * source,target,label
	 * 
	 * @param csvTriple
	 * @throws Exception
	 */
	public StringEdge(String csvTriple, int csvOrder) {
		String[] tokens = VariousUtils.fastSplit(csvTriple, ',');
		switch (csvOrder) {
		case CSV_ORDER_SOURCE_TARGET_LABEL:
			this.source = tokens[0];
			this.target = tokens[1];
			this.label = tokens[2];
			break;
		case CSV_ORDER_LABEL_SOURCE_TARGET:
			this.label = tokens[0];
			this.source = tokens[1];
			this.target = tokens[2];
			break;
		case CSV_ORDER_SOURCE_LABEL_TARGET:
			this.source = tokens[0];
			this.label = tokens[1];
			this.target = tokens[2];
			break;
		default:
			throw new RuntimeException("invalid csv ordering");
		}
	}

	public StringEdge(StringEdge other) {
		this(other.source, other.target, other.label);
	}

	@Override
	public Object clone() {
		return createCopy();
	}

	public StringEdge createCopy() {
		return new StringEdge(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringEdge other = (StringEdge) obj;
		return label.contentEquals(other.label) && // ---
				source.contentEquals(other.source) && // ---
				target.contentEquals(other.target);
	}

	public String getLabel() {
		return label;
	}

	public String getOppositeOf(String v) {
		if (source.equals(v)) {
			return target;
		} else if (target.equals(v)) {
			return source;
		} else {
			return null;
		}
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		if (!hashcodeCached) {
			cacheHashCode();
			hashcodeCached = true;
		}
		return hashcode;
	}

	private void cacheHashCode() {
		final int prime = 127;
		hashcode = 1;
		hashcode = prime * hashcode + label.hashCode();
		hashcode = prime * hashcode + source.hashCode();
		hashcode = prime * hashcode + target.hashCode();
	}

	public byte[] getBytes() {
		String merge = label + "\0" + source + "\0" + target + "\0";
		byte[] byteArray = merge.getBytes(Charset.forName("UTF-8"));
		return byteArray;
	}

	public BigInteger getAsBigInteger() {
//		String merge = label + source + target;
//		byte[] byteArray = merge.getBytes(Charset.forName("UTF-8"));
//		BigInteger bi = new BigInteger(byteArray);
		BigInteger bi = new BigInteger(getBytes());
		return bi;
	}

	/**
	 * returns true if the given reference is the source of this edge, returning false otherwise
	 * 
	 * @param reference
	 * @return
	 */
	public boolean incomesTo(String reference) {
		if (target.equals(reference)) {
			return true;
		} else
			return false;
	}

	/**
	 * returns true if the given reference is the source of this edge, returning false otherwise
	 * 
	 * @param reference
	 * @return
	 */
	public boolean outgoesFrom(String reference) {
		if (source.equals(reference)) {
			return true;
		} else
			return false;
	}

	/**
	 * Creates a new StringEdge with the given oldReference (vertex) replaced with the newReference (vertex). Both source and target may be replaced. VALIDATED.
	 * Remember that this changes the edge, not the graph containing the edge.
	 *
	 * @param oldReference
	 * @param newReference
	 * @return
	 */
	public StringEdge replaceSourceOrTarget(String oldReference, String newReference) {
		StringEdge newEdge = new StringEdge(this);
		if (oldReference.equals(newReference)) // do nothing is this case, duh
			return newEdge;
		if (newEdge.source.equals(oldReference)) {
			newEdge.source = newReference;
		}
		if (newEdge.target.equals(oldReference)) {
			newEdge.target = newReference;
		}
		return newEdge;
	}

	/**
	 * Remember that this changes the edge, not the graph containing the edge.
	 * 
	 * @param oldLabel
	 * @param newLabel
	 * @return
	 */
	public StringEdge replaceLabel(String oldLabel, String newLabel) {
		StringEdge newEdge = new StringEdge(this);
		if (oldLabel.equals(newLabel)) // do nothing is this case, duh
			return newEdge;
		if (newEdge.label.equals(oldLabel)) {
			newEdge.label = newLabel;
		}
		return newEdge;
	}

	/**
	 * Remember that this changes the edge, not the graph containing the edge.
	 * 
	 * @param oldLabel
	 * @param newLabel
	 * @return
	 */
	public StringEdge replaceLabel(String newLabel) {
		StringEdge newEdge = new StringEdge(this);
		newEdge.label = newLabel;
		return newEdge;
	}

	/**
	 * Returns a new edge with the same relation and reversed source / target vertices.
	 * 
	 * @return
	 */
	public StringEdge reverse() {
		return new StringEdge(target, source, label);
	}

	public String toString() {
		return source + "," + label + "," + target;
	}

	public boolean isLoop() {
		return source.equals(target);
	}

	public boolean containsConcept(String reference) {
		if (source.equals(reference) || target.equals(reference))
			return true;
		else
			return false;
	}

	@Override
	public int compareTo(StringEdge o) {
		// return compareToSTL(o);
		return compareToTSL(o);
		// return compareToLST(o);
	}

	public int compareToSTL(StringEdge o) {
		int comp;
		if ((comp = source.compareTo(o.source)) != 0)
			return comp;
		else {
			if ((comp = target.compareTo(o.target)) != 0)
				return comp;
			else {
				return label.compareTo(o.label);
			}
		}
	}

	public int compareToTSL(StringEdge o) {
		int comp;
		if ((comp = target.compareTo(o.target)) != 0)
			return comp;
		else {
			if ((comp = source.compareTo(o.source)) != 0)
				return comp;
			else {
				return label.compareTo(o.label);
			}
		}
	}

	public int compareToLST(StringEdge o) {
		int comp;
		if ((comp = label.compareTo(o.label)) != 0)
			return comp;
		else {
			if ((comp = source.compareTo(o.source)) != 0)
				return comp;
			else {
				return target.compareTo(o.target);
			}
		}
	}

	/**
	 * returns the concept in common between this edge and the given edge, returning null if none
	 * 
	 * @param edge
	 * @return
	 */
	public String getCommonConcept(StringEdge edge) {
		if (this.containsConcept(edge.source))
			return edge.source;
		if (this.containsConcept(edge.target))
			return edge.target;
		return null;
	}

	/**
	 * splits this edge into multiple edges where each blended concept decomposed in two
	 * 
	 * @return
	 */
	public ArrayList<StringEdge> splitBlend() {
		ArrayList<StringEdge> split = new ArrayList<StringEdge>(4);
		if (source.contains("|")) {
			if (target.contains("|")) {
				// TESTED
				// both concepts are blends
				String[] sources = VariousUtils.fastSplit(source, '|');
				String s0 = sources[0];
				String s1 = sources[1];
				String[] targets = VariousUtils.fastSplit(target, '|');
				String t0 = targets[0];
				String t1 = targets[1];
				split.add(new StringEdge(s0, t0, label));
				split.add(new StringEdge(s0, t1, label));
				split.add(new StringEdge(s1, t0, label));
				split.add(new StringEdge(s1, t1, label));
			} else {
				// TESTED
				// only source is blend
				String[] sources = VariousUtils.fastSplit(source, '|');
				String s0 = sources[0];
				String s1 = sources[1];
				split.add(new StringEdge(s0, target, label));
				split.add(new StringEdge(s1, target, label));
			}
		} else {
			// TESTED
			// source is not blend
			if (target.contains("|")) {
				// target is blend
				String[] targets = VariousUtils.fastSplit(target, '|');
				String t0 = targets[0];
				String t1 = targets[1];
				split.add(new StringEdge(source, t0, label));
				split.add(new StringEdge(source, t1, label));
			} else {
				// no concepts are blends
			}
		}
		return split;
	}

	public boolean containsBlendedConcept() {
		return source.contains("|") || target.contains("|"); // lots of vertical bars
	}

	/**
	 * returns true if this edge connects v0 and v1
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public boolean connectsConceptsUndirected(String v0, String v1) {
		if (v0.equals(source) && v1.equals(target))
			return true;
		if (v1.equals(source) && v0.equals(target))
			return true;
		return false;
	}

	/**
	 * returns true if this edge relates v0 to v1 as in source to target
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public boolean connectsConceptsDirected(String v0, String v1) {
		if (v0.equals(source) && v1.equals(target))
			return true;
		return false;
	}

	public boolean targetIsBlend() {
		return target.indexOf('|') != -1;
	}

	public boolean sourceIsBlend() {
		return source.indexOf('|') != -1;
	}

	public boolean containsOnlyBlends() {
		return sourceIsBlend() && targetIsBlend();
	}
}
