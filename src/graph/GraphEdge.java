package graph;

import java.io.Serializable;

public class GraphEdge<V, E> implements Serializable, Cloneable {
	private static final long serialVersionUID = 8432349686429608349L;
	private V source;
	private V target;
	private E label;

	public GraphEdge(V source, V target, E label) {
		this.source = source;
		this.target = target;
		this.label = label;
	}

	public GraphEdge(GraphEdge<V, E> other) {
		this.source = other.source;
		this.target = other.target;
		this.label = other.label;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new GraphEdge<V, E>(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		GraphEdge<V, E> other = (GraphEdge<V, E>) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	public E getLabel() {
		return label;
	}

	public V getOppositeOf(V v) {
		if (source.equals(v)) {
			return target;
		} else if (target.equals(v)) {
			return source;
		} else {
			return null;
		}
	}

	public V getSource() {
		return source;
	}

	public V getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	public boolean incomesTo(V reference) {
		if (target.equals(reference)) {
			return true;
		} else
			return false;
	}

	public boolean outgoesFrom(V reference) {
		if (source.equals(reference)) {
			return true;
		} else
			return false;
	}

	/**
	 * Creates a new GraphEdge with the given oldReference (vertex) replaced with the newReference (vertex). Both source and target may be replaced. VALIDATED.
	 *
	 * @param oldReference
	 * @param newReference
	 * @return
	 */
	public GraphEdge<V, E> replaceSourceOrTarget(V oldReference, V newReference) {
		GraphEdge<V, E> newEdge = new GraphEdge<V, E>(this);
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

	public GraphEdge<V, E> replaceLabel(E oldLabel, E newLabel) {
		GraphEdge<V, E> newEdge = new GraphEdge<V, E>(this);
		if (oldLabel.equals(newLabel)) // do nothing is this case, duh
			return newEdge;
		if (newEdge.label.equals(oldLabel)) {
			newEdge.label = newLabel;
		}
		return newEdge;
	}

	/**
	 * Returns a new edge with the same relation and reversed source / target vertices.
	 * 
	 * @return
	 */
	public GraphEdge<V, E> reverse() {
		return new GraphEdge<V, E>(target, source, label);
	}

	public String toString() {
		return source + "," + label + "," + target;
	}

	public boolean isLoop() {
		return source.equals(target);
	}

	public boolean containsConcept(V reference) {
		if (source.equals(reference) || target.equals(reference))
			return true;
		else
			return false;
	}

}
