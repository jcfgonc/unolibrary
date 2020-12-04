package graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import structures.MapOfSet;
import utils.VariousUtils;

/**
 * High Performance Directed MultiGraph. This graph class does not prevent conflicts of edges. However, used inside the StringGraph (because it uses
 * the StringEdge class) that is not an issue.
 * 
 * @author jcfgonc@gmail.com
 *
 * @param <V> Vertex Class
 * @param <E> Edge Class
 */
public class DirectedMultiGraphOld<V, E> {
	private HashSet<E> edgeSet;
	private HashMap<E, V> edgeSource;
	private HashMap<E, V> edgeTarget;
	private MapOfSet<V, E> incomingEdges;
	private MapOfSet<V, E> outgoingEdges;
	private HashSet<V> vertexSet;
	private static final int DEFAULT_DATA_SIZE = 1 << 8;

	public DirectedMultiGraphOld(int numEdges, int inEdges, int outEdges, int numVertices) {
		edgeSet = new HashSet<>(numEdges);
		edgeSource = new HashMap<>(numEdges);
		edgeTarget = new HashMap<>(numEdges);
		incomingEdges = new MapOfSet<>(inEdges);
		outgoingEdges = new MapOfSet<>(outEdges);
		vertexSet = new HashSet<>(numVertices);
	}

	public DirectedMultiGraphOld() {
		edgeSet = new HashSet<>(DEFAULT_DATA_SIZE);
		edgeSource = new HashMap<>(DEFAULT_DATA_SIZE);
		edgeTarget = new HashMap<>(DEFAULT_DATA_SIZE);
		incomingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE);
		outgoingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE);
		vertexSet = new HashSet<>(DEFAULT_DATA_SIZE);
	}

	/**
	 * returns a new StringGraph with internal structures sized to contain the same data as the given graph
	 * 
	 * @param <V>
	 * @param <E>
	 * @param other
	 * @return
	 */
	public static <V, E> DirectedMultiGraphOld<V, E> allocateSameSize(DirectedMultiGraphOld<V, E> other) {
		return new DirectedMultiGraphOld<V, E>(other.edgeSet.size(), other.incomingEdges.size(), other.outgoingEdges.size(), other.vertexSet.size());
	}

	public void showStructureSizes() {
		System.out.println("edgeSet (number of edges): " + edgeSet.size());
		System.out.println("incomingEdges (number of vertices with incoming edges): " + incomingEdges.size());
		System.out.println("outgoingEdges (number of vertices with outgoing edges): " + outgoingEdges.size());
		System.out.println("vertexSet (number of vertices): " + vertexSet.size());
	}

	public void addEdge(V source, V target, E edge) {
		// this must be prevented outside this function call
		if (containsEdge(edge)) {
			throw new RuntimeException("adding an existing edge");
		}

		incomingEdges.add(target, edge);
		outgoingEdges.add(source, edge);
		edgeSource.put(edge, source);
		edgeTarget.put(edge, target);
		edgeSet.add(edge);

		vertexSet.add(source);
		vertexSet.add(target);
	}

	public boolean containsEdge(E se) {
		return edgeSet.contains(se);
	}

	public boolean containsVertex(V vertex) {
		return vertexSet.contains(vertex);
	}

	public int degreeOf(V vertexId) {
		int d = inDegreeOf(vertexId) + outDegreeOf(vertexId);
		return d;
	}

	/**
	 * unsafe, returns internal set of edges
	 * 
	 * @return
	 */
	public Set<E> edgeSet() {
		return edgeSet;
	}

	public Set<E> edgesOf(V vertex) {
		// union of
		Set<E> in = incomingEdgesOf(vertex);
		// and
		Set<E> out = outgoingEdgesOf(vertex);
		return VariousUtils.mergeSets(in, out);
	}

	/**
	 * get edges outgoing from v0 incoming to v1
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public Set<E> getEdges(V v0, V v1) {
		// intersection of
		Set<E> in = incomingEdgesOf(v1);
		Set<E> out = outgoingEdgesOf(v0);
		// and

		boolean emptyIn = in == null || in.isEmpty();
		boolean emptyOut = out == null || out.isEmpty();

		if (emptyIn || emptyOut) {
			return new HashSet<>(1);
		}

		return VariousUtils.intersection(in, out);

//		Set<E> intersection = new HashSet<>(in);
//		intersection.retainAll(out);
//		return intersection;
	}

	public V getEdgeSource(E edge) {
		return edgeSource.get(edge);
	}

	public V getEdgeTarget(E edge) {
		return edgeTarget.get(edge);
	}

	/**
	 * returns the edges with the target as the given vertex UNTESTED
	 * 
	 * @param vertex
	 * @return
	 */
	public Set<E> incomingEdgesOf(V vertex) {
		Set<E> set = incomingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return new HashSet<>(set);
	}

	public int inDegreeOf(V vertexId) {
		Set<E> i = incomingEdgesOf(vertexId);
		if (i == null) {
			return 0;
		}
		return i.size();
	}

	public int outDegreeOf(V vertexId) {
		Set<E> o = outgoingEdgesOf(vertexId);
		if (o == null) {
			return 0;
		}
		return o.size();
	}

	/**
	 * returns the edges with the source as the given vertex UNTESTED
	 */
	public Set<E> outgoingEdgesOf(V vertex) {
		Set<E> set = outgoingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return new HashSet<>(set);
	}

	public void removeEdge(E edge) {
		if (!containsEdge(edge))
			return;

		V target = getEdgeTarget(edge);
		Set<E> si = incomingEdges.get(target);
		if (si != null) {
			si.remove(edge);
			if (si.isEmpty())
				incomingEdges.removeKey(target);
		}

		V source = getEdgeSource(edge);
		Set<E> so = outgoingEdges.get(source);
		if (so != null) {
			so.remove(edge);
			if (so.isEmpty())
				outgoingEdges.removeKey(source);
		}

		if (degreeOf(source) == 0) {
			vertexSet.remove(source);
		}
		if (degreeOf(target) == 0) {
			vertexSet.remove(target);
		}

		edgeSet.remove(edge);
		edgeSource.remove(edge);
		edgeTarget.remove(edge);
	}

	public void removeEdges(Collection<E> toRemove) {
		for (E edge : toRemove) {
			removeEdge(edge);
		}
	}

	public void removeVertex(V vertex) {
		if (!containsVertex(vertex))
			return;

		for (E edge : incomingEdgesOf(vertex)) {
			removeEdge(edge);
		}

		for (E edge : outgoingEdgesOf(vertex)) {
			removeEdge(edge);
		}

	}

	/**
	 * unsafe, returns internal set of vertices
	 * 
	 * @return
	 */
	public Set<V> vertexSet() {
		return vertexSet;
	}

	public void clear() {
		edgeSet = new HashSet<>(DEFAULT_DATA_SIZE);
		edgeSource = new HashMap<>(DEFAULT_DATA_SIZE);
		edgeTarget = new HashMap<>(DEFAULT_DATA_SIZE);
		incomingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE);
		outgoingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE);
		vertexSet = new HashSet<>(DEFAULT_DATA_SIZE);
	}

	@Override
	public int hashCode() {
		return edgeSet.hashCode();
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
		DirectedMultiGraphOld<V, E> other = (DirectedMultiGraphOld<V, E>) obj;
		if (edgeSet == null) {
			if (other.edgeSet != null)
				return false;
		} else if (!edgeSet.equals(other.edgeSet))
			return false;
		return true;
	}

}
