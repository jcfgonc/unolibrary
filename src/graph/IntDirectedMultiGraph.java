package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.IntMapOfSet;
import structures.ObjectIndex;

/**
 * High Performance Directed MultiGraph. Nodes/vertices are integers (id>0) and edge labels are integers (id>0).
 * 
 * @author jcfgonc@gmail.com
 */
public class IntDirectedMultiGraph {
	private HashSet<IntGraphEdge> edgeSet;
	private Object2IntOpenHashMap<IntGraphEdge> edgeSource;
	private Object2IntOpenHashMap<IntGraphEdge> edgeTarget;
	private IntMapOfSet<IntGraphEdge> incomingEdges;
	private IntMapOfSet<IntGraphEdge> outgoingEdges;
	private IntOpenHashSet vertexSet;

	public IntDirectedMultiGraph() {
		clear();
	}

	public void clear() {
		edgeSet = new HashSet<>();
		edgeSource = new Object2IntOpenHashMap<>();
		edgeTarget = new Object2IntOpenHashMap<>();
		incomingEdges = new IntMapOfSet<>();
		outgoingEdges = new IntMapOfSet<>();
		vertexSet = new IntOpenHashSet();
	}

	public IntDirectedMultiGraph(IntDirectedMultiGraph other) {
		this(other.edgeSet.size(), other.incomingEdges.size(), other.outgoingEdges.size(), other.vertexSet.size());
		addEdges(other.edgeSet());
	}

	public IntDirectedMultiGraph(int numEdges, int inEdges, int outEdges, int numVertices) {
		edgeSet = new HashSet<>(numEdges + numEdges / 2); // 1.5x
		edgeSource = new Object2IntOpenHashMap<>(numEdges + numEdges / 2);
		edgeTarget = new Object2IntOpenHashMap<>(numEdges + numEdges / 2);
		incomingEdges = new IntMapOfSet<>(inEdges + inEdges / 2);
		outgoingEdges = new IntMapOfSet<>(outEdges + outEdges / 2);
		vertexSet = new IntOpenHashSet(numVertices + numVertices / 2);
	}

	public void addEdge(IntGraphEdge ge) {
		addEdge(ge.getSource(), ge.getTarget(), ge.getLabel());
	}

	public void addEdge(int source, int target, int label) {
		assert (source > 0);
		assert (target > 0);
		assert (label > 0);

		if (source == target) {
			System.err.printf("LOOP: %s,%s,%s\n", source, label, target);
			return;
		}

		IntGraphEdge ge = new IntGraphEdge(source, target, label);

		if (edgeSet().contains(ge.reverse())) {
			System.err.printf("SYMMETRY: %s,%s,%s\n", source, label, target);
			return;
		}

		incomingEdges.put(target, ge);
		outgoingEdges.put(source, ge);
		edgeSource.put(ge, source);
		edgeTarget.put(ge, target);
		edgeSet.add(ge);

		vertexSet.add(source);
		vertexSet.add(target);
	}

	public void addEdges(Collection<IntGraphEdge> edges) {
		for (IntGraphEdge edge : edges) {
			addEdge(edge);
		}
	}

	public boolean containsEdge(IntGraphEdge se) {
		return edgeSet.contains(se);
	}

	public boolean containsVertex(int vertex) {
		return vertexSet.contains(vertex);
	}

	public int degreeOf(int vertexId) {
		int d = inDegreeOf(vertexId) + outDegreeOf(vertexId);
		return d;
	}

	/**
	 * unsafe, returns internal set of edges
	 * 
	 * @return
	 */
	public HashSet<IntGraphEdge> edgeSet() {
		return edgeSet;
	}

	public HashSet<IntGraphEdge> edgesOf(int vertex) {
		// union of
		Set<IntGraphEdge> in = incomingEdgesOf(vertex);
		// and
		Set<IntGraphEdge> out = outgoingEdgesOf(vertex);

		int initialCapacity = in.size() + out.size();
		HashSet<IntGraphEdge> set = new HashSet<>(initialCapacity * 2);
		set.addAll(in);
		set.addAll(out);

		return set;
	}

	public HashSet<IntGraphEdge> edgesOf(int vertex, int relation) {
		HashSet<IntGraphEdge> filtered = new HashSet<>();
		Set<IntGraphEdge> edgesOf = edgesOf(vertex);
		for (IntGraphEdge edge : edgesOf) {
			if (edge.getLabel() == relation) {
				filtered.add(edge);
			}
		}
		return filtered;
	}

	public IntOpenHashSet edgesSources(Set<IntGraphEdge> edges) {
		IntOpenHashSet sources = new IntOpenHashSet();
		for (IntGraphEdge edge : edges) {
			int source = edge.getSource();
			sources.add(source);
		}
		return sources;
	}

	public IntOpenHashSet edgesTargets(Set<IntGraphEdge> edges) {
		IntOpenHashSet targets = new IntOpenHashSet();
		for (IntGraphEdge edge : edges) {
			int target = edge.getTarget();
			targets.add(target);
		}
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
	public Set<IntGraphEdge> getEdges(int source, int target, int relation) {
		Set<IntGraphEdge> edges = getEdges(source, target);
		if (edges != null) {
			HashSet<IntGraphEdge> set = new HashSet<>(edges.size() * 2);
			for (IntGraphEdge edge : edges) {
				if (edge.getLabel() == relation)
					set.add(edge);
			}
			return set;
		} else {
			return new HashSet<>(1);
		}
	}

	/**
	 * get edges outgoing from v0 incoming to v1
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public Set<IntGraphEdge> getEdges(int v0, int v1) {
		// intersection of
		Set<IntGraphEdge> in = incomingEdgesOf(v1);
		Set<IntGraphEdge> out = outgoingEdgesOf(v0);
		// and

		boolean emptyIn = in == null || in.isEmpty();
		boolean emptyOut = out == null || out.isEmpty();

		if (emptyIn || emptyOut) {
			return new HashSet<>(1);
		}

		Set<IntGraphEdge> intersection = new HashSet<>(in);
		intersection.retainAll(out);

		return intersection;
	}

	public int getEdgeSource(IntGraphEdge edge) {
		if (!edgeSource.containsKey(edge)) {
			throw new RuntimeException("invalid edge: " + edge);
		}
		return edgeSource.getInt(edge);
	}

	public int getEdgeTarget(IntGraphEdge edge) {
		if (!edgeTarget.containsKey(edge)) {
			throw new RuntimeException("invalid edge: " + edge);
		}
		return edgeTarget.getInt(edge);
	}

	public Set<IntGraphEdge> incomingEdgesOf(int vertex) {
		Set<IntGraphEdge> set = incomingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	public Set<IntGraphEdge> incomingEdgesOf(int concept, int edgeFilter) {
		Set<IntGraphEdge> incoming = incomingEdgesOf(concept);
		HashSet<IntGraphEdge> edges = new HashSet<>(incoming.size());
		for (IntGraphEdge edge : incoming) {
			if (edge.getLabel() == edgeFilter) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public int inDegreeOf(int vertexId) {
		Set<IntGraphEdge> i = incomingEdgesOf(vertexId);
		if (i == null) {
			return 0;
		}
		return i.size();
	}

	public int outDegreeOf(int vertexId) {
		Set<IntGraphEdge> o = outgoingEdgesOf(vertexId);
		if (o == null) {
			return 0;
		}
		return o.size();
	}

	public Set<IntGraphEdge> outgoingEdgesOf(int vertex) {
		Set<IntGraphEdge> set = outgoingEdges.get(vertex);
		if (set == null) {
			return new HashSet<>();
		}
		return set;
	}

	public Set<IntGraphEdge> outgoingEdgesOf(int concept, int edgeFilter) {
		Set<IntGraphEdge> out = outgoingEdgesOf(concept);
		HashSet<IntGraphEdge> edges = new HashSet<>(out.size());
		for (IntGraphEdge edge : out) {
			if (edge.getLabel() == edgeFilter) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public void removeEdge(IntGraphEdge edge) {
		if(!containsEdge(edge))
			return;
		
		int target = getEdgeTarget(edge);
		Set<IntGraphEdge> si = incomingEdges.get(target);
		if (si != null) {
			si.remove(edge);
			if(si.isEmpty())
				incomingEdges.removeKey(target);
		}
		
		int source = getEdgeSource(edge);
		Set<IntGraphEdge> so = outgoingEdges.get(source);
		if (so != null) {
			so.remove(edge);
			if(so.isEmpty())
				outgoingEdges.removeKey(source);
		}

		if (degreeOf(source) == 0) {
			vertexSet.remove(source);
		}
		if (degreeOf(target) == 0) {
			vertexSet.remove(target);
		}

		edgeSet.remove(edge);
		edgeSource.removeInt(edge);
		edgeTarget.removeInt(edge);
	}

	public void removeEdges(Collection<IntGraphEdge> toRemove) {
		for (IntGraphEdge edge : toRemove) {
			removeEdge(edge);
		}
	}

	public void removeVertex(int vertex) {
		if (!containsVertex(vertex))
			return;

		Set<IntGraphEdge> in = incomingEdgesOf(vertex);
		Set<IntGraphEdge> out = outgoingEdgesOf(vertex);

		for (IntGraphEdge edge : in) {
			removeEdge(edge);
		}

		for (IntGraphEdge edge : out) {
			removeEdge(edge);
		}

		incomingEdges.removeKey(vertex);
		outgoingEdges.removeKey(vertex);
		vertexSet.remove(vertex);
	}

	public void removeVertices(Collection<Integer> vertices) {
		for (int v : vertices) {
			removeVertex(v);
		}
	}

	public void removeVertices(IntArrayList vertices) {
		for (int v : vertices) {
			removeVertex(v);
		}
	}

	public void renameVertex(int original, int replacement) {
		if (original == replacement)
			return;
		// remove old edges touching the old vertex while adding new edges with the replaced vertex
		ArrayList<IntGraphEdge> toAdd = new ArrayList<>();
		ArrayList<IntGraphEdge> toRemove = new ArrayList<>();
		for (IntGraphEdge edge : edgesOf(original)) {
			IntGraphEdge newEdge = edge.replaceSourceOrTarget(original, replacement);
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
	public IntOpenHashSet getVertexSet() {
		return vertexSet;
	}

	public static IntDirectedMultiGraph convert(StringGraph graph, ObjectIndex<String> vertexLabels, ObjectIndex<String> relationLabels) {
		IntDirectedMultiGraph converted = new IntDirectedMultiGraph();

		// store indices/dictionaries
		vertexLabels.addObjects(graph.getVertexSet());

		for (StringEdge edge : graph.edgeSet()) {
			relationLabels.addObject(edge.getLabel());
		}

		// add edges using vertex indices and relation labels indices
		for (StringEdge edge : graph.edgeSet()) {
			int sourceId = vertexLabels.getObjectId(edge.getSource());
			int targetId = vertexLabels.getObjectId(edge.getTarget());
			int relationId = relationLabels.getObjectId(edge.getLabel());

			converted.addEdge(sourceId, targetId, relationId);
		}

		return converted;
	}

}
