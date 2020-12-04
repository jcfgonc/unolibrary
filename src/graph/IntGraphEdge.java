package graph;

import java.io.Serializable;

public class IntGraphEdge implements Serializable, Cloneable {
	private static final long serialVersionUID = 8432349686429608350L;
	private int label;
	private int source;
	private int target;

	public IntGraphEdge(int source, int target, int label) {
		this.source = source;
		this.target = target;
		this.label = label;
	}

	public IntGraphEdge(IntGraphEdge other) {
		this.source = other.source;
		this.target = other.target;
		this.label = other.label;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new IntGraphEdge(this);
	}

	public boolean containsConcept(int concept) {
		if (source == concept || target == concept)
			return true;
		else
			return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IntGraphEdge other = (IntGraphEdge) obj;
		if (label != other.label) {
			return false;
		}
		if (source != other.source) {
			return false;
		}
		if (target != other.target) {
			return false;
		}
		return true;
	}

	public int getLabel() {
		return label;
	}

	public int getOppositeOf(int v) {
		if (source == v) {
			return target;
		} else if (target == v) {
			return source;
		} else {
			return -1;
		}
	}

	public int getSource() {
		return source;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + label;
		result = prime * result + source;
		result = prime * result + target;
		return result;
	}

	public boolean incomesTo(int reference) {
		if (target == reference) {
			return true;
		} else
			return false;
	}

	public boolean isLoop() {
		return source == target;
	}

	public boolean outgoesFrom(int reference) {
		if (source == reference) {
			return true;
		} else
			return false;
	}

	public IntGraphEdge replaceLabel(int oldLabel, int newLabel) {
		IntGraphEdge newEdge = new IntGraphEdge(this);
		if (oldLabel == newLabel) // do nothing is this case, duh
			return newEdge;
		if (newEdge.label == oldLabel) {
			newEdge.label = newLabel;
		}
		return newEdge;
	}

	/**
	 * Creates a new IntGraphEdge with the given oldReference (vertex) replaced with the newReference (vertex). Both source and target may be replaced. VALIDATED.
	 *
	 * @param oldReference
	 * @param newReference
	 * @return
	 */
	public IntGraphEdge replaceSourceOrTarget(int oldReference, int newReference) {
		IntGraphEdge newEdge = new IntGraphEdge(this);
		if (oldReference == newReference) // do nothing is this case, duh
			return newEdge;
		if (newEdge.source == oldReference) {
			newEdge.source = newReference;
		}
		if (newEdge.target == oldReference) {
			newEdge.target = newReference;
		}
		return newEdge;
	}

	/**
	 * Returns a new edge with the same relation and reversed source / target vertices.
	 * 
	 * @return
	 */
	public IntGraphEdge reverse() {
		return new IntGraphEdge(target, source, label);
	}

	public String toString() {
		return source + "," + label + "," + target;
	}

}
