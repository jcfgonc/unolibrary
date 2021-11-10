package graph;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;

/**
 * A directed multigraph where both vertices and edges are String. A directed multigraph is a non-simple directed graph in which loops are permitted, as well as
 * multiple edges between any two vertices. Always use the graph's functions to add/remove/etc. edges, never edit directly one of the return functions/values.
 *
 * @author Joao Goncalves: jcfgonc@gmail.com
 *
 *         version 1.3 added containsVertex()
 */
public class StringGraph implements Serializable {

	private static final long serialVersionUID = -1197786236652544325L;
	private static final Set<StringEdge> unmodifiableEmptySet = Collections.unmodifiableSet(new HashSet<StringEdge>(0));

	public static EdgeDirection getEdgeDirectionRelativeTo(String vertex, StringEdge edge) {
		if (isIncoming(vertex, edge))
			return EdgeDirection.INCOMING;
		if (isOutgoing(vertex, edge))
			return EdgeDirection.OUTGOING;
		return EdgeDirection.UNDEFINED;
	}

	public static boolean isIncoming(String vertex, StringEdge edge) {
		if (edge.getTarget().equals(vertex))
			return true;
		return false;
	}

	public static boolean isOutgoing(String vertex, StringEdge edge) {
		if (edge.getSource().equals(vertex))
			return true;
		return false;
	}

	public static boolean isTouching(String vertex, StringEdge edge) {
		if (edge.getTarget().equals(vertex) || edge.getSource().equals(vertex))
			return true;
		else
			return false;
	}

	// DirectedPseudograph<String, StringEdge> graph;
	private DirectedMultiGraphOld<String, StringEdge> graph;
	private final boolean allowSelfLoops = true;
	private final boolean allowSymmetry = true;

	public StringGraph(int numEdges, int inEdges, int outEdges, int numVertices) {
		this.graph = new DirectedMultiGraphOld<String, StringEdge>(numEdges, inEdges, outEdges, numVertices);
	}

	public StringGraph() {
		this.graph = new DirectedMultiGraphOld<String, StringEdge>();
	}

	public StringGraph(StringGraph otherGraph) {
		this.graph = DirectedMultiGraphOld.allocateSameSize(otherGraph.graph);
		addEdges(otherGraph);
	}

	public StringGraph(StringGraph otherGraph, boolean allocateOnly) {
		this.graph = DirectedMultiGraphOld.allocateSameSize(otherGraph.graph);
		if (!allocateOnly) {
			addEdges(otherGraph);
		}
	}

	/**
	 * Converts from generic DirectedMultiGraph to a StringGraph, using toString() on both vertices and edges.
	 * 
	 * @param otherGraph
	 */
	@SuppressWarnings("unchecked")
	public <V, E> StringGraph(DirectedMultiGraphOld<V, E> otherGraph) {
		this.graph = (DirectedMultiGraphOld<String, StringEdge>) DirectedMultiGraphOld.allocateSameSize(otherGraph); // UNTESTED
		for (E edge : otherGraph.edgeSet()) {
			String edgeSource = otherGraph.getEdgeSource(edge).toString();
			String edgeTarget = otherGraph.getEdgeTarget(edge).toString();
			String edgeAsString;

			if (edge instanceof StringEdge) {
				StringEdge e = (StringEdge) edge;
				edgeAsString = e.getLabel();
			} else if (edge instanceof GraphEdge<?, ?>) {
				GraphEdge<?, ?> e = (GraphEdge<?, ?>) edge;
				edgeAsString = e.getLabel().toString();
			} else {
				edgeAsString = edge.toString();
			}

			addEdge(edgeSource, edgeTarget, edgeAsString);
		}
	}

	/**
	 * only copies the edges contained in the given mask
	 * 
	 * @param old
	 * @param mask
	 */
	public StringGraph(StringGraph old, Set<String> mask) {
		this(old, true);
		// TODO: check for divided components
		System.err.println("constructor StringGraph(StringGraph old, Set<String> mask) is to be checked");
		for (StringEdge edge : old.edgeSet()) {
			if (mask.contains(edge.getSource()) && mask.contains(edge.getTarget())) {
				this.addEdge(edge);
			}
		}
	}

	public void addEdges(Collection<StringEdge> edges) {
		for (StringEdge edge : edges) {
			addEdge(edge);
		}
	}

	public void addEdges(StringGraph otherGraph) {
		addEdges(otherGraph.edgeSet());
	}

	public void addEdges(Collection<StringEdge> edges, Set<String> mask) {
		for (StringEdge edge : edges) {
			if (mask.contains(edge.getSource()) && mask.contains(edge.getTarget())) {
				addEdge(edge);
			}
		}
	}

	public void addEdges(StringGraph otherGraph, Set<String> mask) {
		addEdges(otherGraph.edgeSet(), mask);
	}

	/**
	 * adds a the given labeled edge between two vertices, returning true if successfully added it
	 *
	 * @param source
	 * @param target
	 * @param edgeLabel
	 * @return
	 */
	public boolean addEdge(String source, String target, String label) {
		return addEdge(new StringEdge(source, target, label));
	}

	/**
	 * adds the given edge, returning true if successfully added it
	 * 
	 * @param edge
	 * @return
	 */
	public boolean addEdge(StringEdge edge) {

		String source = edge.getSource();
		String target = edge.getTarget();
		String label = edge.getLabel();

//		if (edge.sourceIsBlend() && edge.targetIsBlend())
//			System.lineSeparator();

//		if (anyEdgeConnectsUndirected(source, target)) {
//			Set<StringEdge> edges = getUndirectedEdgesConnecting(source, target);
//			System.lineSeparator();
//		}

		if (containsEdge(edge)) {
//			System.err.printf("edge being added already exists: %s\n", edge);
			return false;
		}

		if (!allowSelfLoops && source.equals(target)) {
//			System.err.printf("LOOP: %s,%s,%s\n", source, label, target);
			return false;
		}

		if (!allowSymmetry && edgeSet().contains(edge.reverse())) {
//			System.err.printf("SYMMETRY: %s,%s,%s\n", source, label, target);
			return false;
		}

		if (source.isEmpty() || target.isEmpty()) {
			throw new RuntimeException(String.format("INVALID SOURCE||TARGET: %s,%s,%s\n", source, label, target));
		}

		if (label.isEmpty()) {
			throw new RuntimeException(String.format("EMPTY RELATION: %s,%s,%s\n", source, label, target));
		}

		this.graph.addEdge(source, target, edge);
		return true;
	}

	/**
	 * clears this graph, removing all vertices and edges
	 */
	public void clear() {
		graph.clear();
	}

	public boolean containsVertex(String vertex) {
		return this.graph.containsVertex(vertex);
	}

	/**
	 * SAFE, returns all the edges of this graph
	 * 
	 * @return
	 */
	public Set<StringEdge> edgeSet() {
		return graph.edgeSet();
	}

	/**
	 * SAFE
	 * 
	 * @param edgeLabel
	 * @return
	 */
	public Set<StringEdge> edgeSet(String edgeLabel) {
		if (isEmpty())
			return unmodifiableEmptySet;

		HashSet<StringEdge> edges = new HashSet<>(edgeSet().size() * 2);
		for (StringEdge edge : edgeSet()) {
			if (edge.getLabel().equals(edgeLabel)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	/**
	 * SAFE, returns both incoming and outgoing edges from the given vertex
	 *
	 * @param vertex
	 * @return
	 */
	public Set<StringEdge> edgesOf(String vertex) {
		return graph.edgesOf(vertex);
	}

	/**
	 * SAFE
	 * 
	 * @param vertex
	 * @param relation
	 * @return
	 */
	public Set<StringEdge> edgesOf(String vertex, String relation) {
		Set<StringEdge> edges = edgesOf(vertex);
		if (edges != null) {

			if (edges.isEmpty())
				return unmodifiableEmptySet;

			Iterator<StringEdge> iterator = edges.iterator();
			while (iterator.hasNext()) {
				StringEdge edge = iterator.next();
				if (!edge.getLabel().equals(relation)) {
					iterator.remove();
				}
			}
		}
		return edges;
	}

	/**
	 * Returns the set of all edges in this graph connecting both vertices, regardless of edge direction.
	 *
	 * @param source
	 * @param target
	 * @return
	 */
	public Set<StringEdge> getUndirectedEdgesConnecting(String vertex0, String vertex1) {
		Set<StringEdge> edgeSet0 = getEdgesConnecting(vertex0, vertex1);
		Set<StringEdge> edgeSet1 = getEdgesConnecting(vertex1, vertex0);
		HashSet<StringEdge> edgeSet = new HashSet<>((edgeSet0.size() + edgeSet1.size()) * 2);
		edgeSet.addAll(edgeSet0);
		edgeSet.addAll(edgeSet1);
		return edgeSet;
	}

	public int getDegree(String concept) {
		return degreeOf(concept);
	}

	/**
	 * Returns the degree (number of connected edges) of the given vertex
	 *
	 * @param concept
	 * @return
	 */
	public int degreeOf(String concept) {
		// return graph.degreeOf(vertexId);
		return graph.degreeOf(concept);
		// return graph.edgesOf(vertexId).size();
	}

	/**
	 * Returns either outgoing or incoming edges to the given vertex
	 *
	 * @param outgoing true if to return outgoing, incoming otherwise
	 * @param concept
	 * @return
	 */
	public Set<StringEdge> getDirectedEdges(boolean outgoing, String concept) {
		if (outgoing)
			return this.outgoingEdgesOf(concept);
		else
			return this.incomingEdgesOf(concept);
	}

	/**
	 * get edges going from source to target
	 *
	 * @param source
	 * @param target
	 * @return
	 */
	public Set<StringEdge> getEdgesConnecting(String source, String target) {
		Set<StringEdge> edgeSet = graph.getEdges(source, target);
		return edgeSet;
	}

	public int getInDegree(String vertexId) {
		return graph.inDegreeOf(vertexId);
	}

	/**
	 * Returns the set of vertices connected to the given vertex
	 *
	 * @param vertex
	 * @return
	 */
	public Set<String> getNeighborVertices(String vertex) {
		Set<StringEdge> edgesI = incomingEdgesOf(vertex);
		Set<StringEdge> edgesO = outgoingEdgesOf(vertex);

		HashSet<String> neighbors = new HashSet<>((edgesI.size() + edgesO.size()) * 2);

		for (StringEdge edge : edgesI) {
			neighbors.add(edge.getSource());
			neighbors.add(edge.getTarget());
		}

		for (StringEdge edge : edgesO) {
			neighbors.add(edge.getSource());
			neighbors.add(edge.getTarget());
		}

		neighbors.remove(vertex);
		return neighbors;
	}

	public Set<String> getIncomingVertices(String vertex) {
		Set<StringEdge> edges = incomingEdgesOf(vertex);
		HashSet<String> sources = edgesSources(edges);
		sources.remove(vertex);
		return sources;
	}

	public Set<String> getOutgoingVertices(String vertex) {
		Set<StringEdge> edges = outgoingEdgesOf(vertex);
		HashSet<String> targets = edgesTargets(edges);
		targets.remove(vertex);
		return targets;
	}

	/**
	 * SAFE
	 * 
	 * @param edges
	 * @return
	 */
	public static HashSet<String> edgesSources(Set<StringEdge> edges) {
		HashSet<String> sources = new HashSet<>(edges.size() * 2);
		for (StringEdge edge : edges) {
			String source = edge.getSource();
			sources.add(source);
		}
		return sources;
	}

	/**
	 * SAFE
	 * 
	 * @param edges
	 * @return
	 */
	public static HashSet<String> edgesTargets(Set<StringEdge> edges) {
		HashSet<String> targets = new HashSet<>(edges.size() * 2);
		for (StringEdge edge : edges) {
			String target = edge.getTarget();
			targets.add(target);
		}
		return targets;
	}

	public int getOutDegree(String vertexId) {
		return graph.outDegreeOf(vertexId);
	}

	/**
	 * Returns the set of vertices contained in this graph and which participate in edges.
	 *
	 * @return
	 */
	public Set<String> getVertexSet() {
		Set<String> vertexSet = this.graph.vertexSet();
		return vertexSet;
	}

	/**
	 * SAFE, returns the edges with the target as the given vertex
	 * 
	 * @param vertex
	 * @return
	 */
	public Set<StringEdge> incomingEdgesOf(String vertex) {
		return graph.incomingEdgesOf(vertex);
	}

	/**
	 * SAFE, returns the edges with the target as the given vertex
	 * 
	 * @param concept
	 * @param filter
	 * @return
	 */
	public Set<StringEdge> incomingEdgesOf(String concept, String filter) {
		Set<StringEdge> incoming = incomingEdgesOf(concept);
		HashSet<StringEdge> edges = new HashSet<>(incoming.size() * 2);
		for (StringEdge edge : incoming) {
			if (edge.getLabel().equals(filter)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public int numberOfEdges() {
		return graph.edgeSet().size();
	}

	public int numberOfEdges(String label) {
		int num = 0;
		for (StringEdge edge : edgeSet()) {
			if (edge.getLabel().equals(label)) {
				num++;
			}
		}
		return num;
	}

	public int numberOfVertices() {
		return graph.vertexSet().size();
	}

	/**
	 * SAFE, returns the edges with the source as the given vertex
	 * 
	 * @param vertex
	 * @return
	 */
	public Set<StringEdge> outgoingEdgesOf(String vertex) {
		return graph.outgoingEdgesOf(vertex);
	}

	/**
	 * SAFE, returns the edges with the source as the given vertex
	 * 
	 * @param concept
	 * @param filter
	 * @return
	 */
	public Set<StringEdge> outgoingEdgesOf(String concept, String filter) {
		Set<StringEdge> out = outgoingEdgesOf(concept);
		HashSet<StringEdge> edges = new HashSet<>(out.size() * 2);
		for (StringEdge edge : out) {
			if (edge.getLabel().equals(filter)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public void removeEdge(StringEdge edgeToDelete) {
		graph.removeEdge(edgeToDelete);
	}

	public void removeEdge(String source, String target, String label) {
		removeEdge(new StringEdge(source, target, label));
	}

	public void removeLoops() {
		Set<StringEdge> edges = this.edgeSet();
		ArrayList<StringEdge> toRemove = new ArrayList<>();
		for (StringEdge edge : edges) {
			if (edge.isLoop()) {
				toRemove.add(edge);
			}
		}

		for (StringEdge edge : toRemove) {
			this.removeEdge(edge);
		}
	}

	/**
	 * Removes the specified vertex from this graph including all its touching edges if present.
	 *
	 * @param vertex
	 * @return true if the graph contained the specified vertex; false otherwise.
	 */
	public void removeVertex(String vertex) {
		graph.removeVertex(vertex);
	}

	public void removeVertices(Collection<String> vertices) {
		for (String v : vertices) {
			removeVertex(v);
		}
	}

	/**
	 * VALIDATED.
	 * 
	 * @param edge
	 * @param original
	 * @param replacement
	 */
	public void replaceEdgeSourceOrTarget(StringEdge edge, String original, String replacement) {
		StringEdge newEdge = edge.replaceSourceOrTarget(original, replacement);
		removeEdge(edge);
		addEdge(newEdge);
	}

	/**
	 * @param original
	 * @param replacement
	 */
	public void replaceVertex(String original, String replacement) {
		renameVertex(original, replacement);
	}

	public void renameVertex(String original, String replacement) {
		if (original.equals(replacement))
			return;
		// remove old edges touching the old vertex while adding new edges with the replaced vertex
		ArrayList<StringEdge> toAdd = new ArrayList<>();
		ArrayList<StringEdge> toRemove = new ArrayList<>();
		for (StringEdge edge : graph.edgesOf(original)) {
			StringEdge newEdge = edge.replaceSourceOrTarget(original, replacement);
			toAdd.add(newEdge);
			toRemove.add(edge);
		}
		removeEdges(toRemove);
		addEdges(toAdd);
	}

	public String toString(final int limit, final int lineBreak) {
		if (isEmpty())
			return "[]";

		int counter = 0;
		String buffer = "";
		for (StringEdge edge : edgeSet()) {
			buffer += edge.toString() + ";";
			if (counter % lineBreak == 0 && counter > 0)
				buffer += System.lineSeparator();
			if (counter > limit)
				break;
			counter++;
		}
		return buffer;
	}

	public String toString() {
		return toString(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public void removeEdges(Collection<StringEdge> toRemove) {
		graph.removeEdges(toRemove);
	}

	public boolean containsEdge(StringEdge se) {
		return graph.containsEdge(se);
	}

	public boolean containsEdge(String source, String target, String label) {
		return containsEdge(new StringEdge(source, target, label));
	}

	public void showStructureSizes() {
		graph.showStructureSizes();
	}

	/**
	 * Returns the set of relations/labels in this graph's edges.
	 * 
	 * @return
	 */
	public HashSet<String> getEdgeLabelSet() {
		HashSet<String> edgeLabels = new HashSet<>(16, 0.333f);
		for (StringEdge edge : edgeSet()) {
			edgeLabels.add(edge.getLabel());
		}
		return edgeLabels;
	}

	/**
	 * Returns true if this graph contains no edges, false otherwise.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return edgeSet().isEmpty();
	}

	/**
	 * Removes all the edges containing the given relation/label.
	 * 
	 * @param relationFilter
	 */
	public void removeEdgesByLabel(String relationFilter) {
		Set<StringEdge> edges = edgeSet(relationFilter);
		removeEdges(edges);
	}

	public void removeEdgesByLabel(Collection<String> labels) {
		for (String label : labels) {
			removeEdgesByLabel(label);
		}
	}

	/**
	 * SAFE, returns the edges touching this edge's source and target vertices
	 * 
	 * @param edge
	 * @return
	 */
	public HashSet<StringEdge> edgesOf(StringEdge edge) {
		String source = edge.getSource();
		String target = edge.getTarget();

		Set<StringEdge> sourceEdges = this.edgesOf(source);
		Set<StringEdge> targetEdges = this.edgesOf(target);

		HashSet<StringEdge> touching = new HashSet<>((sourceEdges.size() + targetEdges.size()) * 2);
		touching.addAll(sourceEdges);
		touching.addAll(targetEdges);
		touching.remove(edge);

		return touching;
	}

	/**
	 * SAFE
	 * 
	 * @param vertices
	 * @return
	 */
	public HashSet<StringEdge> edgesOf(Collection<String> vertices) {
		HashSet<StringEdge> edges = new HashSet<StringEdge>(1 << 10);
		for (String vertex : vertices) {
			edges.addAll(edgesOf(vertex));
		}
		return edges;
	}

	/**
	 * SAFE
	 * 
	 * @param vertices
	 * @param filter
	 * @return
	 */
	public HashSet<StringEdge> edgesOf(Set<String> vertices, String filter) {
		HashSet<StringEdge> edges = new HashSet<StringEdge>(1 << 10);
		for (String vertex : vertices) {
			edges.addAll(edgesOf(vertex, filter));
		}
		return edges;
	}

	@Override
	public int hashCode() {
		return graph.hashCode();
	}

	private void addBytesToArrayList(ByteArrayList byteList, byte[] bytes) {
		for (byte b : bytes) {
			byteList.add(b);
		}
	}

	public byte[] accurateHashCode() {
		ByteArrayList byteList = new ByteArrayList();
		for (StringEdge edge : edgeSet()) {
			// addBytesToArrayList(byteList, edge.getHashedBytes());
			addBytesToArrayList(byteList, edge.getBytes());
		}
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException nsae) {
			throw new InternalError("SHA-256 not supported", nsae);
		}
		byte[] d = md.digest(byteList.toByteArray());
		return d;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringGraph other = (StringGraph) obj;
		return graph.equals(other.graph);
	}

	/**
	 * returns true if there is an edge connecting v0 to v1 or vice-versa UNTESTED
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public boolean anyEdgeConnectsUndirected(String v0, String v1) {
		return anyEdgeConnectsDirected(v0, v1) || anyEdgeConnectsDirected(v1, v0);
	}

	/**
	 * returns true if there is an edge connecting the same vertices as the given edge or vice-versa UNTESTED
	 */
	public boolean anyEdgeConnectsUndirected(StringEdge e) {
		return anyEdgeConnectsUndirected(e.getSource(), e.getTarget());
	}

	/**
	 * returns true if there is an edge connecting from v0 to v1 UNTESTED
	 * 
	 * @param v0
	 * @param v1
	 * @return
	 */
	public boolean anyEdgeConnectsDirected(String v0, String v1) {
		for (StringEdge edge : outgoingEdgesOf(v0)) {
			if (edge.getTarget().equals(v1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if there is an edge connecting the same vertices as the given edge and in the same direction
	 */
	public boolean anyEdgeConnectsDirected(StringEdge e) {
		return anyEdgeConnectsDirected(e.getSource(), e.getTarget());
	}

	public void removeVerticesStartingWith(String prefix) {
		ArrayList<String> toRemove = new ArrayList<String>();
		for (String vertex : getVertexSet()) {
			if (vertex.startsWith(prefix))
				toRemove.add(vertex);
		}
		removeVertices(toRemove);
	}

}
