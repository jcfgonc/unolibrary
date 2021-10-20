package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import structures.MapOfSet;

/**
 * High Performance Directed MultiGraph.
 * 
 * @author jcfgonc@gmail.com
 *
 * @param <V> Vertex Class
 * @param <E> Edge Class
 */
public class DirectedMultiGraph<V, E> {
	private HashSet<GraphEdge<V, E>> edgeSet;
	private HashMap<GraphEdge<V, E>, V> edgeSource;
	private HashMap<GraphEdge<V, E>, V> edgeTarget;
	private MapOfSet<V, GraphEdge<V, E>> incomingEdges;
	private MapOfSet<V, GraphEdge<V, E>> outgoingEdges;
	private HashSet<V> vertexSet;

	public DirectedMultiGraph() {
		edgeSet = new HashSet<>();
		edgeSource = new HashMap<>();
		edgeTarget = new HashMap<>();
		incomingEdges = new MapOfSet<>();
		outgoingEdges = new MapOfSet<>();
		vertexSet = new HashSet<>();
	}

	public DirectedMultiGraph(DirectedMultiGraph<V, E> other) {
		this(other.edgeSet.size(), other.incomingEdges.size(), other.outgoingEdges.size(), other.vertexSet.size());
		addEdges(other.edgeSet());
	}

	public DirectedMultiGraph(int numEdges, int inEdges, int outEdges, int numVertices) {
		edgeSet = new HashSet<>(numEdges + numEdges / 2);
		edgeSource = new HashMap<>(numEdges + numEdges / 2);
		edgeTarget = new HashMap<>(numEdges + numEdges / 2);
		incomingEdges = new MapOfSet<>(inEdges + inEdges / 2);
		outgoingEdges = new MapOfSet<>(outEdges + outEdges / 2);
		vertexSet = new HashSet<>(numVertices + numVertices / 2);
	}

	public void addEdge(GraphEdge<V, E> ge) {
		addEdge(ge.getSource(), ge.getTarget(), ge.getLabel());
	}

	public void addEdge(V source, V target, E label) {

		GraphEdge<V, E> ge = new GraphEdge<V, E>(source, target, label);

		if (source.equals(target)) {
			System.err.printf("LOOP: %s,%s,%s\n", source, label, target);
			return;
		}

		if (edgeSet().contains(ge.reverse())) {
			System.err.printf("SYMMETRY: %s,%s,%s\n", source, label, target);
			return;
		}

		incomingEdges.add(target, ge);
		outgoingEdges.add(source, ge);
		edgeSource.put(ge, source);
		edgeTarget.put(ge, target);
		edgeSet.add(ge);

		vertexSet.add(source);
		vertexSet.add(target);
	}

	public void addEdges(Collection<GraphEdge<V, E>> edges) {
		for (GraphEdge<V, E> edge : edges) {
			addEdge(edge);
		}
	}

	public void clear() {
		edgeSet = new HashSet<>();
		edgeSource = new HashMap<>();
		edgeTarget = new HashMap<>();
		incomingEdges = new MapOfSet<>();
		outgoingEdges = new MapOfSet<>();
		vertexSet = new HashSet<>();
	}

	public boolean containsEdge(GraphEdge<V, E> se) {
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
	public HashSet<GraphEdge<V, E>> edgeSet() {
		return edgeSet;
	}

	public HashSet<GraphEdge<V, E>> edgesOf(V vertex) {
		// union of
		Set<GraphEdge<V, E>> in = incomingEdgesOf(vertex);
		// and
		Set<GraphEdge<V, E>> out = outgoingEdgesOf(vertex);

		int initialCapacity = in.size() + out.size();
		HashSet<GraphEdge<V, E>> set = new HashSet<>(initialCapacity * 2);
		set.addAll(in);
		set.addAll(out);

		return set;
	}

	public HashSet<GraphEdge<V, E>> edgesOf(V vertex, E relation) {
		HashSet<GraphEdge<V, E>> filtered = new HashSet<>();
		Set<GraphEdge<V, E>> edgesOf = edgesOf(vertex);
		for (GraphEdge<V, E> edge : edgesOf) {
			if (edge.getLabel().equals(relation)) {
				filtered.add(edge);
			}
		}
		return filtered;
	}

	private HashSet<V> edgesSources(Set<GraphEdge<V, E>> edges) {
		HashSet<V> sources = new HashSet<>();
		for (GraphEdge<V, E> edge : edges) {
			V source = edge.getSource();
			sources.add(source);
		}
		return sources;
	}

	private HashSet<V> edgesTargets(Set<GraphEdge<V, E>> edges) {
		HashSet<V> targets = new HashSet<>();
		for (GraphEdge<V, E> edge : edges) {
			V target = edge.getTarget();
			targets.add(target);
		}
		// this may contain the specified vertex, so remove it
		return targets;
	}

	/**
	 * Returns the set of all edges in this graph connecting the given source to the given target with the given relation.
	 *
	 * @param source
	 * @param target
	 * @param relation
	 * @return
	 */
	public Set<GraphEdge<V, E>> getDirectedEdgesWithRelationEqualTo(V source, V target, E relation) {
		HashSet<GraphEdge<V, E>> set = new HashSet<>();
		Set<GraphEdge<V, E>> edges = getEdges(source, target);
		if (edges != null) {
			for (GraphEdge<V, E> edge : edges) {
				if (edge.getLabel().equals(relation))
					set.add(edge);
			}
		}
		return set;
	}

	/**
	 * get edges outgoing from v0 incoming to v1
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public Set<GraphEdge<V, E>> getEdges(V v0, V v1) {
		// intersection of
		Set<GraphEdge<V, E>> in = incomingEdgesOf(v1);
		Set<GraphEdge<V, E>> out = outgoingEdgesOf(v0);
		// and

		boolean emptyIn = in == null || in.isEmpty();
		boolean emptyOut = out == null || out.isEmpty();

		if (emptyIn || emptyOut) {
			return new HashSet<>(1);
		}

		Set<GraphEdge<V, E>> intersection = new HashSet<>(in);
		intersection.retainAll(out);

		return intersection;
	}

	public V getEdgeSource(GraphEdge<V, E> edge) {
		return edgeSource.get(edge);
	}

	public V getEdgeTarget(GraphEdge<V, E> edge) {
		return edgeTarget.get(edge);
	}

	public Set<V> getIncomingVertices(V vertex) {
		Set<GraphEdge<V, E>> edges = incomingEdgesOf(vertex);
		HashSet<V> sources = edgesSources(edges);
		// this may contain the specified vertex, so remove it
		sources.remove(vertex);
		return sources;
	}

	public Set<V> getOutgoingVertices(V vertex) {
		Set<GraphEdge<V, E>> edges = outgoingEdgesOf(vertex);
		HashSet<V> targets = edgesTargets(edges);
		targets.remove(vertex);
		return targets;
	}

	public Set<GraphEdge<V, E>> incomingEdgesOf(V vertex) {
		Set<GraphEdge<V, E>> set = incomingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	public Set<GraphEdge<V, E>> incomingEdgesOf(V concept, E filter) {
		Set<GraphEdge<V, E>> incoming = incomingEdgesOf(concept);
		HashSet<GraphEdge<V, E>> edges = new HashSet<>(incoming.size());
		for (GraphEdge<V, E> edge : incoming) {
			if (edge.getLabel().equals(filter)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public int inDegreeOf(V vertexId) {
		Set<GraphEdge<V, E>> i = incomingEdgesOf(vertexId);
		if (i == null) {
			return 0;
		}
		return i.size();
	}

	public int outDegreeOf(V vertexId) {
		Set<GraphEdge<V, E>> o = outgoingEdgesOf(vertexId);
		if (o == null) {
			return 0;
		}
		return o.size();
	}

	public Set<GraphEdge<V, E>> outgoingEdgesOf(V vertex) {
		Set<GraphEdge<V, E>> set = outgoingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	public Set<GraphEdge<V, E>> outgoingEdgesOf(V concept, E filter) {
		Set<GraphEdge<V, E>> out = outgoingEdgesOf(concept);
		HashSet<GraphEdge<V, E>> edges = new HashSet<>(out.size());
		for (GraphEdge<V, E> edge : out) {
			if (edge.getLabel().equals(filter)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public void removeEdge(GraphEdge<V, E> edge) {
		if (!containsEdge(edge))
			return;

		V target = getEdgeTarget(edge);
		Set<GraphEdge<V, E>> si = incomingEdges.get(target);
		if (si != null) {
			si.remove(edge);
			if (si.isEmpty())
				incomingEdges.removeKey(target);
		}

		V source = getEdgeSource(edge);
		Set<GraphEdge<V, E>> so = outgoingEdges.get(source);
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

	public void removeEdges(Collection<GraphEdge<V, E>> toRemove) {
		for (GraphEdge<V, E> edge : toRemove) {
			removeEdge(edge);
		}
	}

	public void removeVertex(V vertex) {
		if (!containsVertex(vertex))
			return;

		Set<GraphEdge<V, E>> in = incomingEdgesOf(vertex);
		Set<GraphEdge<V, E>> out = outgoingEdgesOf(vertex);

		ArrayList<GraphEdge<V, E>> list = new ArrayList<>(in.size() + out.size());

		list.addAll(in);
		list.addAll(out);

		for (GraphEdge<V, E> edge : list) {
			removeEdge(edge);
		}
	}

	public void removeVertices(Collection<V> vertices) {
		for (V v : vertices) {
			removeVertex(v);
		}
	}

	public void renameVertex(V original, V replacement) {
		if (original.equals(replacement))
			return;
		// remove old edges touching the old vertex while adding new edges with the replaced vertex
		ArrayList<GraphEdge<V, E>> toAdd = new ArrayList<>();
		ArrayList<GraphEdge<V, E>> toRemove = new ArrayList<>();
		for (GraphEdge<V, E> edge : edgesOf(original)) {
			GraphEdge<V, E> newEdge = edge.replaceSourceOrTarget(original, replacement);
			toAdd.add(newEdge);
			toRemove.add(edge);
		}
		removeEdges(toRemove);
		addEdges(toAdd);
	}

	public void showStructureSizes() {
		System.out.println("edgeSet: " + edgeSet.size());
		System.out.println("incomingEdges: " + incomingEdges.size());
		System.out.println("outgoingEdges: " + outgoingEdges.size());
		System.out.println("vertexSet: " + vertexSet.size());
	}

	/**
	 * unsafe, returns internal set of vertices
	 * 
	 * @return
	 */
	public Set<V> vertexSet() {
		return vertexSet;
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
		DirectedMultiGraph<V, E> other = (DirectedMultiGraph<V, E>) obj;
		return edgeSet.equals(other.edgeSet);
	}

	public int getNumberOfVertices() {
		return vertexSet.size();
	}

	public int getNumberOfEdges() {
		return edgeSet.size();
	}

	public String toString() {
		return toString(Integer.MAX_VALUE, Integer.MAX_VALUE);

	}

	public String toString(final int limit, final int lineBreak) {
		if (isEmpty())
			return "[]";

		int counter = 0;
		String buffer = "";
		for (GraphEdge<V, E> edge : edgeSet()) {
			buffer += edge.toString() + ";";
			if (counter % lineBreak == 0 && counter > 0)
				buffer += System.lineSeparator();
			if (counter > limit)
				break;
			counter++;
		}
		return buffer;
	}

	public boolean isEmpty() {
		return getNumberOfEdges() == 0;
	}

	public int getNumberOfEdges(String label) {
		int count = 0;
		for (GraphEdge<V, E> edge : edgeSet) {
			if (edge.getLabel().equals(label))
				count++;
		}
		return count;
	}

}
