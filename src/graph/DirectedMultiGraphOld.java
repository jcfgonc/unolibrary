package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import structures.MapOfSet;
import utils.VariousUtils;

/**
 * High Performance Directed MultiGraph. This graph class does not prevent conflicts of edges. However, used inside the StringGraph (because it uses the
 * StringEdge class) that is not an issue.
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
	private static final int DEFAULT_DATA_SIZE = 16;
	private static final float DEFAULT_LOAD_FACTOR = 0.5f;
	private final Set<E> unmodifiableEmptyEdgeSet = Collections.unmodifiableSet(new HashSet<E>(0));

	public DirectedMultiGraphOld(int numEdges, int inEdges, int outEdges, int numVertices) {
		edgeSet = new HashSet<>(numEdges * 2, DEFAULT_LOAD_FACTOR);
		edgeSource = new HashMap<>(numEdges * 2, DEFAULT_LOAD_FACTOR);
		edgeTarget = new HashMap<>(numEdges * 2, DEFAULT_LOAD_FACTOR);
		incomingEdges = new MapOfSet<>(inEdges * 2, DEFAULT_LOAD_FACTOR);
		outgoingEdges = new MapOfSet<>(outEdges * 2, DEFAULT_LOAD_FACTOR);
		vertexSet = new HashSet<>(numVertices * 2, DEFAULT_LOAD_FACTOR);
	}

	public DirectedMultiGraphOld() {
		edgeSet = new HashSet<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
		edgeSource = new HashMap<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
		edgeTarget = new HashMap<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
		incomingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
		outgoingEdges = new MapOfSet<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
		vertexSet = new HashSet<>(DEFAULT_DATA_SIZE, DEFAULT_LOAD_FACTOR);
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
		int numEdges = other.edgeSet.size();
		int inEdges = other.incomingEdges.size();
		int outEdges = other.outgoingEdges.size();
		int numVertices = other.vertexSet.size();
		return new DirectedMultiGraphOld<V, E>(numEdges, inEdges, outEdges, numVertices);
	}

	public void showStructureSizes() {
		System.out.println("edgeSet (number of edges): " + edgeSet.size());
		System.out.println("incomingEdges (number of vertices with incoming edges): " + incomingEdges.size());
		System.out.println("outgoingEdges (number of vertices with outgoing edges): " + outgoingEdges.size());
		System.out.println("vertexSet (number of vertices): " + vertexSet.size());
	}

	public void addEdge(V source, V target, E edge) {
		// this must be prevented outside this function call
//		if (containsEdge(edge)) {
//			throw new RuntimeException("adding an existing edge");
//		}

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
	 * SAFE, returns the set of edges of this graph
	 * 
	 * @return
	 */
	public Set<E> edgeSet() {
		if (edgeSet.isEmpty())
			return unmodifiableEmptyEdgeSet;
		return Collections.unmodifiableSet(edgeSet);
	}

	protected Set<E> edgeSet_unsafe() {
		if (edgeSet.isEmpty())
			return unmodifiableEmptyEdgeSet;
		return edgeSet;
	}

	/**
	 * SAFE, returns the edges touching (both incoming and outgoing to) the given vertex
	 * 
	 * @param vertex
	 * @return
	 */
	public Set<E> edgesOf(V vertex) {
		// union of
		Set<E> in = incomingEdgesOf(vertex);
		// and
		Set<E> out = outgoingEdgesOf(vertex);
		return VariousUtils.mergeSets(in, out);
	}

	/**
	 * SAFE, get edges outgoing from v0 incoming to v1
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
			return unmodifiableEmptyEdgeSet;
		}

		return VariousUtils.intersection(in, out);
	}

	public V getEdgeSource(E edge) {
		return edgeSource.get(edge);
	}

	public V getEdgeTarget(E edge) {
		return edgeTarget.get(edge);
	}

	/**
	 * SAFE, returns the edges with the target as the given vertex
	 * 
	 * @param vertex
	 * @return
	 */
	public Set<E> incomingEdgesOf(V vertex) {
		Set<E> set = incomingEdges.get(vertex);
		if (set == null || set.isEmpty()) {
			return unmodifiableEmptyEdgeSet;
		}
		return Collections.unmodifiableSet(set);
	}

	public int inDegreeOf(V vertex) {
		Set<E> i = incomingEdgesOf(vertex);
		if (i == null) {// it is null when the given vertex has no incoming edges
			return 0;
		}
		return i.size();
	}

	public int outDegreeOf(V vertex) {
		Set<E> o = outgoingEdgesOf(vertex);
		if (o == null) { // it is null when the given vertex has no outgoing edges
			return 0;
		}
		return o.size();
	}

	/**
	 * SAFE, returns the edges with the source as the given vertex
	 */
	public Set<E> outgoingEdgesOf(V vertex) {
		Set<E> set = outgoingEdges.get(vertex);
		if (set == null || set.isEmpty()) {
			return unmodifiableEmptyEdgeSet;
		}
		return Collections.unmodifiableSet(set);
	}

	public void removeEdge(E edge) {
		if (!containsEdge(edge))
			return;

		V target = getEdgeTarget(edge);
		Set<E> si = incomingEdges.get(target);
		if (si != null) {
			incomingEdges.remove(target, edge);
			if (si.isEmpty())
				incomingEdges.removeKey(target);
		}

		V source = getEdgeSource(edge);
		Set<E> so = outgoingEdges.get(source);
		if (so != null) {
			outgoingEdges.remove(source, edge);
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

		ArrayList<E> touchingEdges = new ArrayList<E>();
		touchingEdges.addAll(incomingEdgesOf(vertex));
		touchingEdges.addAll(outgoingEdgesOf(vertex));

		removeEdges(touchingEdges);

	}

	/**
	 * SAFE, returns internal set of vertices
	 * 
	 * @return
	 */
	public Set<V> vertexSet() {
		return Collections.unmodifiableSet(vertexSet);
	}

	protected Set<V> vertexSet_unsafe() {
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
