package graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.ConceptPair;
import structures.ListOfSet;
import structures.MapOfSet;
import structures.ObjectIndex;
import structures.OrderedPair;
import structures.UnorderedPair;
import utils.VariousUtils;

public class GraphAlgorithms {
	public interface ExpandingEdge {
		public void expanding(String from, String to);
	}

	public static MapOfSet<String, StringEdge> createNameSpaceToEdgeSet(StringGraph inputSpace_) {
		MapOfSet<String, StringEdge> nameSpaceEdges = new MapOfSet<String, StringEdge>();
		Set<StringEdge> edgeSet = inputSpace_.edgeSet();
		for (StringEdge edge : edgeSet) {
			// String label = edge.getLabel();
			String source = edge.getSource();
			String target = edge.getTarget();

			String sourceNS = getConceptNamespace(source);
			String targetNS = getConceptNamespace(target);

			// these two should be the same namespace but you never know
			nameSpaceEdges.add(sourceNS, edge);
			nameSpaceEdges.add(targetNS, edge);
		}
		// System.out.printf("createNameSpaceToEdgeSet() got %d namespaces :
		// %s\n", nameSpaceEdges.size(), nameSpaceEdges.keySet());
		return nameSpaceEdges;
	}

	public static MapOfSet<String, String> createNameSpaceToConceptSet(StringGraph inputSpace) {
		MapOfSet<String, String> nameSpaces = new MapOfSet<>();
		// scan for namespaces, stored as namespace/concept
		for (String concept : inputSpace.getVertexSet()) {
			String currentNameSpace = getConceptNamespace(concept);
			nameSpaces.add(currentNameSpace, concept);
		}
		// System.out.printf("createNameSpaceToConceptSet() got %d namespaces :
		// %s\n", nameSpaces.size(), nameSpaces.keySet());
		return nameSpaces;
	}

	/**
	 * Expands on the graph, from the openset, excluding nodes present in the closed set, returning the new expanded nodes. Whenever a new edge is being
	 * expanded from the current set to a neighboring node, ExpandingEdge ee is invoked.
	 *
	 * @param openSet
	 * @param closedSet
	 * @param graph
	 * @return
	 */
	public static Set<String> expandFromOpenSetOneLevel(Set<String> openSet, Set<String> closedSet, StringGraph graph, ExpandingEdge ee) {
		// changes to be done to open and closed sets (to prevent concurrent
		// modification exception)
		Set<String> openSetAddition = new HashSet<String>(16, 0.333f);
		Set<String> openSetRemoval = new HashSet<String>(16, 0.333f);
		// for each vertex in the open set not in the closed set
		for (String vertexId : openSet) {
			if (closedSet.contains(vertexId))
				continue;
			// get the vertex neighbors not in the closed set
			Set<String> neighbors = graph.getNeighborVertices(vertexId);
			for (String neighborId : neighbors) {
				if (closedSet.contains(neighborId))
					continue;
				// put the neighbors in the open set
				openSetAddition.add(neighborId);
				if (ee != null)
					ee.expanding(vertexId, neighborId);
			}
			// vertex from the open set explored, remove it from further
			// exploration
			openSetRemoval.add(vertexId);
			closedSet.add(vertexId);
		}
		// do the changes in the open and closed sets
		openSet.addAll(openSetAddition);
		openSet.removeAll(openSetRemoval);
		return openSetAddition;
	}

	/**
	 * Expands on the graph, from the openset, excluding nodes present in the closed set, returning the new expanded nodes. Whenever a new edge is being
	 * expanded from the current set to a neighboring node, ExpandingEdge ee is invoked.
	 *
	 * @param openSet
	 * @param closedSet
	 * @param graph
	 * @return
	 */
	public static Set<String> expandFromVertexOneLevel(String vertexId, Set<String> closedSet, StringGraph graph, ExpandingEdge ee) {
		// changes to be done to open and closed sets (to prevent concurrent
		// modification exception)
		Set<String> openSetAddition = new HashSet<String>(16, 0.333f);
		Set<String> openSetRemoval = new HashSet<String>(16, 0.333f);
		// not in the closed set
		if (!closedSet.contains(vertexId)) {
			// get the vertex neighbors not in the closed set
			Set<String> neighbors = graph.getNeighborVertices(vertexId);
			for (String neighborId : neighbors) {
				if (closedSet.contains(neighborId))
					continue;
				// put the neighbors in the open set
				openSetAddition.add(neighborId);
				if (ee != null)
					ee.expanding(vertexId, neighborId);
			}
			// vertex from the open set explored, remove it from further
			// exploration
			openSetRemoval.add(vertexId);
			closedSet.add(vertexId);
		}
		return openSetAddition;
	}

	public static Map<String, String> createConceptToNameSpaceMap(StringGraph genericSpace) {
		HashMap<String, String> conceptToNS = new HashMap<>();
		for (String concept : genericSpace.getVertexSet()) {
			String namespace = getConceptNamespace(concept);
			conceptToNS.put(concept, namespace);
		}
		return conceptToNS;
	}

	/**
	 * Creates a new graph whose vertices are contained in the given namespaces.
	 *
	 * @param graph
	 * @param namespaces
	 * @return
	 */
	public static StringGraph filterNamespaces(StringGraph graph, Collection<String> namespaces) {
		StringGraph out = new StringGraph();
		Set<StringEdge> edgeSet = graph.edgeSet();
		for (StringEdge edge : edgeSet) {
			String label = edge.getLabel();
			String source = edge.getSource();
			String target = edge.getTarget();

			String sourceNS = getConceptNamespace(source);
			String targetNS = getConceptNamespace(target);
			if (namespaces.contains(sourceNS) && namespaces.contains(targetNS)) {
				out.addEdge(source, target, label);
				// System.out.printf("%s %s %s\n",source, target, label);
			}
		}
		return out;
	}

	private static final HashMap<String, String> conceptToNamespace = new HashMap<>();
	private static final ReentrantLock rlock0 = new ReentrantLock();

	public static String getConceptNamespace(String concept) {
		String namespace = null;
		rlock0.lock();
		try {
			namespace = conceptToNamespace.get(concept);
		} finally {
			rlock0.unlock();
		}

		if (namespace != null)
			return namespace;

		int i0 = concept.indexOf('/');
		if (i0 < 0)
			return null;
		namespace = concept.substring(0, i0);

		rlock0.lock();
		try {
			conceptToNamespace.put(concept, namespace);
		} finally {
			rlock0.unlock();
		}
		return namespace;
	}

	private static final HashMap<String, String> fullNameToConcept = new HashMap<>();
	private static final ReentrantLock rlock1 = new ReentrantLock();

	public static String getConceptWithoutNamespace(String fullConcept) {
		String concept = null;
		rlock1.lock();
		try {
			concept = fullNameToConcept.get(fullConcept);
		} finally {
			rlock1.unlock();
		}

		if (concept != null)
			return concept;

		int i0 = fullConcept.indexOf('/');
		if (i0 < 0)
			return fullConcept;
		concept = fullConcept.substring(i0 + 1);

		rlock1.lock();
		try {
			fullNameToConcept.put(fullConcept, concept);
		} finally {
			rlock1.unlock();
		}
		return concept;
	}

	/**
	 * returns a random vertex from depth <limit> starting from the given vertex
	 *
	 * @param random
	 * @param startingVertex
	 * @param graph
	 * @param limit
	 * @return
	 */
	public static String getDeepRandomVertex(RandomGenerator random, String startingVertex, StringGraph graph, int limit) {
		String currentvertex = startingVertex;
		for (int i = 0; i < limit; i++) {
			Set<String> neighborhood = graph.getNeighborVertices(currentvertex);
			if (neighborhood.isEmpty()) {
				return currentvertex;
			}
			currentvertex = VariousUtils.getRandomElementFromCollection(neighborhood, random);
		}
		return currentvertex;
	}

	public static String getVertexFromRandomWalk(RandomGenerator random, String startingVertex, StringGraph graph, int limit) {
		String currentvertex = startingVertex;
		for (int i = 0; i < limit; i++) {
			Set<StringEdge> edgesOf = graph.edgesOf(currentvertex);
			if (edgesOf.isEmpty()) {
				return currentvertex;
			}
			StringEdge nextEdge = VariousUtils.getRandomElementFromCollection(edgesOf, random);
			currentvertex = nextEdge.getOppositeOf(currentvertex);
		}
		return currentvertex;
	}

	public static int getDistance(StringGraph graph, String origin, String destination, int maxDistance) {
		// set of visited and to visit vertices for the expansion process
		// expand from vertex0 until arriving at vertex1
		Set<String> openSet = new HashSet<String>(1024, 0.333f);
		Set<String> closedSet = new HashSet<String>(1024, 0.333f);
		openSet.add(origin);

		int distance = 0;
		do {
			// only expand while there are vertices to expand
			if (openSet.size() == 0)
				break;
			// reached destination?
			if (openSet.contains(destination))
				break;
			// only expand if allowed
			if (distance >= maxDistance)
				return Integer.MAX_VALUE;
			// do one expansion
			expandFromOpenSetOneLevel(openSet, closedSet, graph, null);
			distance++;
		} while (true);
		return distance;
	}

	public static HashMap<String, ArrayList<StringEdge>> lowestCommonAncestorIsa(StringGraph graph, String vertexL, String vertexR, boolean useDerivedFrom,
			boolean useSynonym) {
		HashMap<String, StringEdge> cameFromEdgeL = new HashMap<>();
		HashMap<String, StringEdge> cameFromEdgeR = new HashMap<>();
		HashMap<String, ArrayList<StringEdge>> ancestors = new HashMap<>();

		// do the left-right breadth first expansion
		{
			HashSet<String> closedSetL = new HashSet<>(16, 0.333f);
			ArrayDeque<String> openSetL = new ArrayDeque<>();
			HashSet<String> touchedL = new HashSet<>(16, 0.333f);

			HashSet<String> closedSetR = new HashSet<>(16, 0.333f);
			ArrayDeque<String> openSetR = new ArrayDeque<>();
			HashSet<String> touchedR = new HashSet<>(16, 0.333f);

			openSetL.addLast(vertexL);
			openSetR.addLast(vertexR);
			while (openSetL.size() > 0 || openSetR.size() > 0) {
				{
					HashSet<String> expanded = lcaIsaRadialExpansion(graph, openSetL.removeFirst(), useDerivedFrom, useSynonym, cameFromEdgeL, closedSetL);
					openSetL.addAll(expanded);
					touchedL.addAll(expanded);
				}

				{
					HashSet<String> expanded = lcaIsaRadialExpansion(graph, openSetR.removeFirst(), useDerivedFrom, useSynonym, cameFromEdgeR, closedSetR);
					openSetR.addAll(expanded);
					touchedR.addAll(expanded);
				}
				HashSet<String> collision = VariousUtils.intersection(touchedL, touchedR);
				if (collision.size() > 0) {
					System.currentTimeMillis();
					for (String ref : collision) {
						ArrayList<StringEdge> pathL = getEdgePath(ref, cameFromEdgeL);
						ArrayList<StringEdge> pathR = getEdgePath(ref, cameFromEdgeR);
						ArrayList<StringEdge> fullPath = new ArrayList<>(pathL.size() + pathR.size());
						Collections.reverse(pathL);
						fullPath.addAll(pathL);
						fullPath.addAll(pathR);
						// remove repeated edges
						// TODO: feels like a stupid solution
						fullPath = removeRepeatedEdges(fullPath);

						String ancestor = validateAndGetAncestorFromSequence(fullPath, vertexL, vertexR);
						if (ancestor != null) {
							ancestors.put(ancestor, fullPath);
						}
					}

					if (!ancestors.isEmpty()) {
						break;
					}
				}
			}
		}
		return ancestors;
	}

	public static ArrayList<StringEdge> removeRepeatedEdges(ArrayList<StringEdge> edges) {
		ArrayList<StringEdge> newEdges = new ArrayList<>(edges.size());
		HashSet<StringEdge> closedSet = new HashSet<>(16, 0.333f);
		for (StringEdge edge : edges) {
			if (closedSet.contains(edge))
				continue;
			closedSet.add(edge);
			newEdges.add(edge);
		}
		return newEdges;
	}

	/**
	 * @param fullPath [horse,isa,equinae, equinae,isa,animal, aves,isa,animal, bird,isa,aves]
	 * @param vertexL  horse
	 * @param vertexR  bird
	 * @return animal
	 */
	private static String validateAndGetAncestorFromSequence(ArrayList<StringEdge> fullPath, String vertexL, String vertexR) {
		HashSet<String> lastConcepts = new HashSet<>();
		lastConcepts.add(vertexL);

		boolean outgoingPhase = true;
		int reversedDirection = 0;
		String ancestor = null;

		// TODO: this fails with repeated edges
		for (StringEdge edge : fullPath) {
			String source = edge.getSource();
			String target = edge.getTarget();
			String relation = edge.getLabel();
			if (relation.equals("isa")) {

				// outgoing (to the right)
				if (outgoingPhase) {
					// still outgoing?
					if (lastConcepts.contains(source)) {
						lastConcepts.clear();
						lastConcepts.add(target);
					} else
					// changed direction?
					if (lastConcepts.contains(target)) {
						lastConcepts.clear();
						lastConcepts.add(source);
						outgoingPhase = false;
						reversedDirection++;
						ancestor = target;
					} else {
						System.err.println("TODO: 2795");
					}
				}

				// incoming (to the right)
				else {
					// still incoming?
					if (lastConcepts.contains(target)) {
						lastConcepts.clear();
						lastConcepts.add(source);
					} else
					// changed direction?
					if (lastConcepts.contains(source)) {
						lastConcepts.clear();
						lastConcepts.add(target);
						outgoingPhase = true;
						reversedDirection++;
					} else {
						System.err.println("TODO: 2796");
					}
				}

			} else {
				lastConcepts.add(source);
				lastConcepts.add(target);
			}
		}

		if (reversedDirection % 2 == 0 || !lastConcepts.contains(vertexR)) {
			return null;
		}
		return ancestor;
	}

	/**
	 * Path is REVERSED
	 * 
	 * @param endingConcept
	 * @param conceptCameFromEdge
	 * @return
	 */
	public static ArrayList<StringEdge> getEdgePath(String endingConcept, HashMap<String, StringEdge> conceptCameFromEdge) {
		HashSet<StringEdge> closedSet = new HashSet<>();
		ArrayList<StringEdge> path = new ArrayList<>();
		StringEdge source;
		String current = endingConcept;
		while (true) {
			source = conceptCameFromEdge.get(current);
			// System.out.println(current + "\t" + (source == null ? "[]" : source));
			if (source == null)
				break;
			if (!closedSet.contains(source)) {
				path.add(source);
			}
			closedSet.add(source);
			current = source.getOppositeOf(current);
		}
		return path;
	}

	/**
	 * used by function lowestCommonAncestorIsa()
	 * 
	 * @param graph
	 * @param currentVertex
	 * @param useDerivedFrom
	 * @param useSynonym
	 * @param cameFromEdge
	 * @param closedSetConcepts
	 * @return
	 */
	private static HashSet<String> lcaIsaRadialExpansion(StringGraph graph, String currentVertex, boolean useDerivedFrom, boolean useSynonym,
			HashMap<String, StringEdge> cameFromEdge, HashSet<String> closedSetConcepts) {
		HashSet<String> newOpenSet = new HashSet<>();
		// stop when left and right expansions collide
		// expand edges with vertices not in the closed set
		if (!closedSetConcepts.contains(currentVertex)) {
			closedSetConcepts.add(currentVertex);
			Set<StringEdge> edges = graph.edgesOf(currentVertex);
			for (StringEdge edge : edges) {
				String edgeLabel = edge.getLabel();
				if (edgeLabel.equals("isa") && edge.outgoesFrom(currentVertex) || // ISA are always outgoing
						edgeLabel.equals("derivedfrom") && useDerivedFrom || // DERIVEDFROM &
						edgeLabel.equals("synonym") && useSynonym) { // SYNONYM are bidirectional
					String neighbor = edge.getOppositeOf(currentVertex);
					if (closedSetConcepts.contains(neighbor))
						continue;

					if (!cameFromEdge.containsKey(neighbor)) {
						cameFromEdge.put(neighbor, edge);
					}
					// put the neighbors in the open set
					newOpenSet.add(neighbor);
				}
			}
		}
		return newOpenSet;
	}

	public static ArrayList<StringEdge> shortestIsaPath(StringGraph graph, String start, String toReach, boolean useDerivedFrom, boolean useSynonym) {
		HashMap<String, StringEdge> cameFromEdge = new HashMap<>();

		// TODO: closed and open sets should be for edges, not concepts (to allow further exploration using alternative paths)
		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		openSet.addLast(start);

		while (!openSet.isEmpty()) {
			// get next vertex
			String currentVertex = openSet.removeFirst();
			// if we arrived at the destination, abort expansion
			// if (currentVertex.equals(toReach))
			// break;

			ArrayList<StringEdge> path = getEdgePath(currentVertex, cameFromEdge);
			// must contain at least one isa
			if (!path.isEmpty() && pathContainsConcept(toReach, path) && pathContainsConcept(toReach, path)) {
				Collections.reverse(path);
				return path;
			}

			// expand a vertex not in the closed set
			if (!closedSet.contains(currentVertex)) {
				// get the vertex neighbors not in the closed set

				Set<StringEdge> out = graph.edgesOf(currentVertex);
				for (StringEdge edge : out) {
					String neighborId = null;
					String label = edge.getLabel();
					if (label.equals("isa")) {
						neighborId = edge.getTarget();
					} else if (label.equals("derivedfrom") && useDerivedFrom) {
						neighborId = edge.getOppositeOf(currentVertex);
					} else if (label.equals("synonym") && useSynonym) {
						neighborId = edge.getOppositeOf(currentVertex);
					}
					if (neighborId == null || neighborId.equals(currentVertex) || closedSet.contains(neighborId))
						continue;
					if (!cameFromEdge.containsKey(neighborId)) {
						cameFromEdge.put(neighborId, edge);
					}
					// put the neighbors in the open set
					openSet.addLast(neighborId);
					// if (closedSet.contains(neighborId))
					// continue;
				}
				// vertex from the open set explored, remove it from further
				// exploration
				closedSet.add(currentVertex);
			}
		}
		return new ArrayList<>(1);
	}

	public static boolean pathContainsConcept(String concept, ArrayList<StringEdge> path) {
		for (StringEdge edge : path) {
			if (edge.containsConcept(concept)) {
				return true;
			}
		}
		return false;
	}

	public static boolean edgePathContainsISA(ArrayList<StringEdge> path) {
		boolean containsIsa = false;
		for (StringEdge edge : path) {
			if (edge.getLabel().equals("isa")) {
				containsIsa = true;
				break;
			}
		}
		return containsIsa;
	}

	/**
	 * TODO: dont know if this is 100% correct
	 * 
	 * @param graph
	 * @param start
	 * @param toReach
	 */
	public static void shortestPathSearch(StringGraph graph, String start, String toReach) {
		HashMap<String, String> cameFrom = new HashMap<>();

		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		openSet.addLast(start);

		while (!openSet.isEmpty()) {
			// get next vertex
			String currentVertex = openSet.removeFirst();
			// if we arrived at the destination, abort expansion
			if (currentVertex.equals(toReach))
				break;
			// expand a vertex not in the closed set
			if (!closedSet.contains(currentVertex)) {
				// get the vertex neighbors not in the closed set
				// TODO: melhor getOutgoingVertices e caso falha, pesquisa ao contrario
				Set<String> neighbors = graph.getNeighborVertices(currentVertex);
				for (String neighborId : neighbors) {
					if (closedSet.contains(neighborId))
						continue;
					if (!cameFrom.containsKey(neighborId)) {
						cameFrom.put(neighborId, currentVertex);
					}
					// put the neighbors in the open set
					openSet.addLast(neighborId);
				}
				// vertex from the open set explored, remove it from further
				// exploration
				closedSet.add(currentVertex);
			}
		}

		// get path
		String source;
		String current = toReach;
		while (true) {
			source = cameFrom.get(current);
			System.out.println(current);
			System.out.println(graph.getUndirectedEdgesConnecting(source, current));
			if (source == null)
				break;
			current = source;
		}
	}

	/**
	 * Returns sorted list of sets of concepts (components)
	 * 
	 * @param graph
	 * @return
	 */
	public static ListOfSet<String> extractGraphComponents(StringGraph graph) {
		ListOfSet<String> graphComponents = new ListOfSet<>();
		HashSet<String> potentialSet = new HashSet<>(graph.getVertexSet());
		while (potentialSet.size() > 0) {
			// just get a vertex
			String firstVertex = potentialSet.iterator().next();
			HashSet<String> closedSet = new HashSet<>(16, 0.333f);
			HashSet<String> openSet = new HashSet<>(16, 0.333f);
			// start in a given vertex
			openSet.add(firstVertex);
			// expand all neighbors
			// when it stops, you get an island
			while (openSet.size() > 0) {
				Set<String> newVertices = GraphAlgorithms.expandFromOpenSetOneLevel(openSet, closedSet, graph, null);
				if (newVertices.isEmpty())
					break;
				openSet.addAll(newVertices);
				openSet.removeAll(closedSet);
			}
			// one more component done
			graphComponents.add(closedSet);
			potentialSet.removeAll(closedSet);
		}
		graphComponents.sortList(false);
		return graphComponents;
	}

	/**
	 * removes all all components except the biggest one, done in-place.
	 * 
	 * @param graph
	 */
	public static void removeSmallerComponents(StringGraph graph) {
		ListOfSet<String> components = extractGraphComponents(graph);
		// vertices in the largest component
		HashSet<String> componentVertices = components.getSetAt(0);
		// graph vertices in a array for better caching
		ArrayList<String> graphVertices = new ArrayList<String>(graph.getVertexSet());
		// remove from graph the vertices not contained in the component
		for (String vertice : graphVertices) {
			if (componentVertices.contains(vertice))
				continue;
			graph.removeVertex(vertice);
		}
	}

	/**
	 * Extracts a connected set (component) from the given graph.
	 * 
	 * @param graph
	 * @param minNewConceptsTrigger
	 * @param minTotalConceptsTrigger
	 * @param random
	 * @return
	 */
	public static HashSet<String> extractRandomPart(StringGraph graph, int minNewConceptsTrigger, int minTotalConceptsTrigger, RandomGenerator random) {
		// just get a vertex
		String firstVertex = VariousUtils.getRandomElementFromCollection(graph.getVertexSet(), random);
		HashSet<String> closedSet = new HashSet<>(16, 0.333f);
		HashSet<String> openSet = new HashSet<>(16, 0.333f);
		// start in a given vertex
		openSet.add(firstVertex);
		// ---
		while (openSet.size() > 0) {
			// do a radial expansion
			Set<String> newVertices = GraphAlgorithms.expandFromOpenSetOneLevel(openSet, closedSet, graph, null);
			if (newVertices.isEmpty())
				break;

			if (closedSet.size() > minTotalConceptsTrigger) {
				break;
			}

			if (newVertices.size() > minNewConceptsTrigger) {
				newVertices = VariousUtils.randomSubSet(newVertices, minNewConceptsTrigger, random);
			}

			openSet.addAll(newVertices);
			openSet.removeAll(closedSet);
		}
		return closedSet;
	}

	public static Set<String> getNeighborhoodDepth(String from, int maxDepth, StringGraph graph) {
		Set<String> openSet = new HashSet<String>(16, 0.333f);
		Set<String> openSetRemoval = new HashSet<String>(16, 0.333f);
		Set<String> openSetAddition = new HashSet<String>(16, 0.333f);
		Set<String> closedSet = new HashSet<String>(16, 0.333f);
		openSet.addAll(graph.getNeighborVertices(from));
		closedSet.add(from);
		for (int currentDepth = 1; currentDepth < maxDepth; currentDepth++) {
			for (String vertexId : openSet) {
				if (closedSet.contains(vertexId))
					continue;
				Set<String> neighbors = graph.getNeighborVertices(vertexId);
				for (String neighborId : neighbors) {
					if (closedSet.contains(neighborId))
						continue;
					openSetAddition.add(neighborId);
				}
				openSetRemoval.add(vertexId);
				closedSet.add(vertexId);
			}
			openSet.addAll(openSetAddition);
			openSet.removeAll(openSetRemoval);
			openSetAddition.clear();
			openSetRemoval.clear();
		}
		return openSet;
	}

	public static StringGraph intersectGraphWithVertexSet(StringGraph graph, Set<String> maskingVertexSet) {
		StringGraph graphCopy = new StringGraph(graph);
		// get list of vertices from graph
		Set<String> graphVertexSet = graph.getVertexSet();
		// iterate vertices from graph
		for (String graphVertex : graphVertexSet) {
			// for each graph vertice not in the mask set
			if (!maskingVertexSet.contains(graphVertex)) {
				// -remove vertex and associated edges from the graph
				graphCopy.removeVertex(graphVertex);
			}
		}
		return graphCopy;
	}

	/**
	 * was splitConceptWithBar (using vertical '|')
	 * 
	 * @param concept
	 * @return
	 */
	public static ArrayList<String> splitConcept(String concept, String separator) {
		int bar_i = concept.indexOf(separator);
		String concept0 = concept.substring(0, bar_i);
		String concept1 = concept.substring(bar_i + 1);
		ArrayList<String> split = new ArrayList<>(2);
		split.add(concept0);
		split.add(concept1);
		return split;
	}

	public static HashSet<String> getNameSpaces(StringGraph graph) {
		Set<String> concepts = graph.getVertexSet();
		HashSet<String> namespaces = new HashSet<>(16, 0.333f);
		for (String concept : concepts) {
			String namespace = GraphAlgorithms.getConceptNamespace(concept);
			if (!namespaces.contains(namespace)) {
				namespaces.add(namespace);
			}
		}
		return namespaces;
	}

	/**
	 * self-explanatory
	 * 
	 * @param vertexSet - the set of vertices to search (ie, mask on the graph)
	 * @param graph     - the StringGraph containing the edges
	 * @return
	 */
	public static String getHighestDegreeVertex(Collection<String> vertexSet, StringGraph graph) {
		int highestDegree = -Integer.MAX_VALUE;
		String highestDegreeConcept = null;
		for (String concept : vertexSet) {
			int degree = graph.degreeOf(concept);
			if (degree > highestDegree) {
				highestDegree = degree;
				highestDegreeConcept = concept;
			}
		}
		return highestDegreeConcept;
	}

	public static String getHighestDegreeVertex(StringGraph graph) {
		return getHighestDegreeVertex(graph.getVertexSet(), graph);
	}

	public static <V, E> V getHighestDegreeVertex(DirectedMultiGraph<V, E> graph) {
		int highestDegree = -Integer.MAX_VALUE;
		V highestDegreeVertex = null;
		for (V vertex : graph.vertexSet()) {
			int degree = graph.degreeOf(vertex);
			if (degree > highestDegree) {
				highestDegree = degree;
				highestDegreeVertex = vertex;
			}
		}
		return highestDegreeVertex;
	}

	public static <V, E> V getLowestDegreeVertex(DirectedMultiGraph<V, E> graph) {
		int lowestDegree = Integer.MAX_VALUE;
		V lowestDegreeVertex = null;
		for (V vertex : graph.vertexSet()) {
			int degree = graph.degreeOf(vertex);
			if (degree < lowestDegree) {
				lowestDegree = degree;
				lowestDegreeVertex = vertex;
			}
		}
		return lowestDegreeVertex;
	}

	/**
	 * self-explanatory
	 * 
	 * @param vertexSet - the set of vertices to search (ie, mask on the graph)
	 * @param graph     - the StringGraph containing the edges
	 * @return
	 */
	public static String getLowestDegreeVertex(Collection<String> vertexSet, StringGraph graph) {
		int lowestDegree = Integer.MAX_VALUE;
		String lowestDegreeConcept = null;
		for (String concept : vertexSet) {
			int degree = graph.degreeOf(concept);
			if (degree < lowestDegree) {
				lowestDegree = degree;
				lowestDegreeConcept = concept;
			}
		}
		return lowestDegreeConcept;
	}

	public static String getLowestDegreeVertex(StringGraph graph) {
		return getLowestDegreeVertex(graph.getVertexSet(), graph);
	}

	public static ArrayList<StringEdge> getEdgesWithSources(Collection<StringEdge> edges, Collection<String> collection) {
		ArrayList<StringEdge> inCommon = new ArrayList<>(edges.size());
		for (StringEdge edge : edges) {
			if (collection.contains(edge.getSource()))
				inCommon.add(edge);
		}
		return inCommon;
	}

	public static ArrayList<String> getEdgesSources(Collection<StringEdge> edges) {
		ArrayList<String> sources = new ArrayList<>(edges.size());
		for (StringEdge edge : edges) {
			sources.add(edge.getSource());
		}
		return sources;
	}

	public static HashSet<String> getEdgesSourcesAsSet(Collection<StringEdge> edges) {
		HashSet<String> sources = new HashSet<>(edges.size());
		for (StringEdge edge : edges) {
			sources.add(edge.getSource());
		}
		return sources;
	}

	public static ArrayList<String> getEdgesTargets(Collection<StringEdge> edges) {
		ArrayList<String> sources = new ArrayList<>(edges.size());
		for (StringEdge edge : edges) {
			sources.add(edge.getTarget());
		}
		return sources;
	}

	public static HashSet<String> getEdgesTargetsAsSet(Collection<StringEdge> edges) {
		HashSet<String> sources = new HashSet<>(edges.size());
		for (StringEdge edge : edges) {
			sources.add(edge.getTarget());
		}
		return sources;
	}

	public static ArrayList<String> getEdgesLabels(Collection<StringEdge> edges) {
		ArrayList<String> strings = new ArrayList<>(edges.size());
		for (StringEdge edge : edges) {
			strings.add(edge.getLabel());
		}
		return strings;
	}

	public static HashSet<String> getEdgesLabelsAsSet(Collection<StringEdge> edges) {
		HashSet<String> strings = new HashSet<>(edges.size());
		for (StringEdge edge : edges) {
			strings.add(edge.getLabel());
		}
		return strings;
	}

	public static <V, E> HashSet<E> getEdgesLabelsAsSet(Set<GraphEdge<V, E>> edges) {
		HashSet<E> strings = new HashSet<>(edges.size());
		for (GraphEdge<V, E> edge : edges) {
			strings.add(edge.getLabel());
		}
		return strings;
	}

	/**
	 * Converts a string graph using textual concepts/relations to a graph of integers.
	 * 
	 * @param graph
	 * @param vertexLabels
	 * @param relationLabels
	 * @return
	 */
	public static IntDirectedMultiGraph convertStringGraph2IntDirectedMultiGraph(StringGraph graph, // --
			ObjectIndex<String> vertexLabels, // --
			ObjectIndex<String> relationLabels) {
		IntDirectedMultiGraph out = new IntDirectedMultiGraph(1 << 20, 1 << 20, 1 << 20, 1 << 20);
		for (StringEdge edge : graph.edgeSet()) {
			int sourceId = vertexLabels.addObject(edge.getSource());
			int targetId = vertexLabels.addObject(edge.getTarget());
			int relationId = relationLabels.addObject(edge.getLabel());

			out.addEdge(sourceId, targetId, relationId);
		}
		return out;
	}

	public static StringGraph convertIntDirectedMultiGraph2StringGraph(IntDirectedMultiGraph graph, // --
			ObjectIndex<String> vertexLabels, // --
			ObjectIndex<String> relationLabels) {
		StringGraph out = new StringGraph(1 << 20, 1 << 20, 1 << 20, 1 << 20);
		for (IntGraphEdge edge : graph.edgeSet()) {
			String sourceId = vertexLabels.getObject(edge.getSource());
			String targetId = vertexLabels.getObject(edge.getTarget());
			String relationId = relationLabels.getObject(edge.getLabel());

			out.addEdge(sourceId, targetId, relationId);
		}
		return out;
	}

	/**
	 * returns the histogram of the graph edge's relations
	 * 
	 * @param graph
	 * @return
	 */
	public static Object2IntOpenHashMap<String> countRelations(StringGraph graph) {
		return countRelations(graph.edgeSet());
	}

	/**
	 * returns the histogram of the edge's relations
	 * 
	 */
	public static Object2IntOpenHashMap<String> countRelations(Set<StringEdge> edges) {
		Object2IntOpenHashMap<String> counter = new Object2IntOpenHashMap<>();
		counter.defaultReturnValue(0);
		for (StringEdge edge : edges) {
			String relation = edge.getLabel();
			counter.addTo(relation, 1);
		}
		return counter;
	}

	public static Object2IntOpenHashMap<String> countRelations(DirectedMultiGraph<OrderedPair<String>, String> graph) {
		return countRelations(graph.edgeSet());
	}

	private static Object2IntOpenHashMap<String> countRelations(HashSet<GraphEdge<OrderedPair<String>, String>> edges) {
		Object2IntOpenHashMap<String> counter = new Object2IntOpenHashMap<>();
		counter.defaultReturnValue(0);
		for (GraphEdge<OrderedPair<String>, String> edge : edges) {
			String relation = edge.getLabel();
			counter.addTo(relation, 1);
		}
		return counter;
	}

	/**
	 * returns the statistics of the graph edge's relations
	 * 
	 * @param graph
	 * @return
	 */
	public static DescriptiveStatistics getRelationStatistics(StringGraph graph) {
		return getRelationStatistics(countRelations(graph));
	}

	public static DescriptiveStatistics getRelationStatistics(Object2IntOpenHashMap<String> count) {
		int numRelations = count.size();
		if (numRelations == 0) {
			throw new RuntimeException("empty relation histogram, unable to compute statistics");
		}
		double[] count_d = new double[numRelations];
		int i = 0;
		for (String key : count.keySet()) {
			count_d[i++] = count.getInt(key);
		}
		DescriptiveStatistics ds = new DescriptiveStatistics(count_d);
		return ds;
	}

	public static DescriptiveStatistics getRelationStatisticsNormalized(Object2IntOpenHashMap<String> count, double divider) {
		int numRelations = count.size();
		double[] count_d = new double[numRelations];
		int i = 0;
		for (String key : count.keySet()) {
			count_d[i] = (double) count.getInt(key) / divider;
			i++;
		}
		DescriptiveStatistics ds = new DescriptiveStatistics(count_d);
		return ds;
	}

	/**
	 * reads something of the sort "slave=X1,no_choice_or_freedom=X0,own=X2,master=X3" into a hashmap
	 * 
	 * @param string
	 * @return
	 */
	public static HashMap<String, String> readMap(String string) {
		HashMap<String, String> map = new HashMap<>();
		String[] pairs = VariousUtils.fastSplit(string, ',');
		for (String pair : pairs) {
			String[] elements = VariousUtils.fastSplit(pair, '=');
			map.put(elements[0], elements[1]);
		}
		return map;
	}

	/**
	 * according to the replaceTo0 flag, either swaps vertice <b>from</b> with vertice <b>to0</b> or vertice <b>from</b> with vertice <b>to1</b>
	 * 
	 * @param replaceTo0
	 * @param graph
	 * @param from
	 * @param to0
	 * @param to1
	 */
	public static void swapVertex(boolean replaceTo0, StringGraph graph, String from, String to0, String to1) {
		if (replaceTo0) {
			graph.renameVertex(from, to0);
		} else {
			graph.renameVertex(from, to1);
		}
	}

	/**
	 * removes from the edge set a the edges which vertices are also connected in the edge set b
	 * 
	 * @param a
	 * @param b
	 * @return a copy of the edge set a with the removed edges described above
	 */
	public static Set<StringEdge> removeConnectedVertices(Set<StringEdge> a, Set<StringEdge> b) {
		if (b.isEmpty()) {
			return a;
		}

		if (a.isEmpty()) {
			return new HashSet<StringEdge>();
		}

		HashSet<StringEdge> filtered_a = new HashSet<StringEdge>(16, 0.333f);
		for (StringEdge edge : a) {
			boolean connected = areVerticesConnectedUndirected(b, edge.getSource(), edge.getTarget());
			if (!connected) {
				filtered_a.add(edge);
			} else {
				// System.lineSeparator();
			}
		}
		return filtered_a;
	}

	/**
	 * returns true if the given edge set contains an edge connected between the vertices v0 and v1 (or v1 and v0)
	 * 
	 * @param set
	 * @param v0
	 * @param v1
	 * @return
	 */
	public static boolean areVerticesConnectedUndirected(Set<StringEdge> set, String v0, String v1) {
		for (StringEdge edge : set) {
			if (edge.connectsConceptsUndirected(v0, v1)) {
				return true;
			}
		}
		return false;
	}

	public static int countCycles(StringGraph pattern) {
		// this only works if the graph has one component

		// TODO: adapt this to support multiple components
		ArrayDeque<StringEdge> edgesToVisit = new ArrayDeque<>();
		// TODO if there are errors in cycle counting replace unorderedpair with orderedpair
		HashSet<UnorderedPair<String>> edgesVisited = new HashSet<>(16, 0.333f);
		HashSet<String> verticesVisited = new HashSet<>(16, 0.333f);

		StringEdge startingEdge = pattern.edgeSet().iterator().next();
		edgesToVisit.add(startingEdge);
		int cycles = 0;

		while (true) {
			StringEdge edge = edgesToVisit.pollLast();
			if (edge == null)
				break;
			String source = edge.getSource();
			String target = edge.getTarget();
			// check if the vertex pair has been already visited
			UnorderedPair<String> edgeUndirected = new UnorderedPair<String>(source, target);
			if (edgesVisited.contains(edgeUndirected))
				continue;
			edgesVisited.add(edgeUndirected);

			boolean sourceVisited = verticesVisited.contains(source);
			boolean targetVisited = verticesVisited.contains(target);
			if (sourceVisited && targetVisited) {
				cycles++;
			}

			verticesVisited.add(source);
			verticesVisited.add(target);

			// do not add coincident edges, i.e., guarantee unique vertex pairs
			HashSet<StringEdge> edgesTouching = pattern.edgesOf(edge);
			for (StringEdge newEdge : edgesTouching) {
				UnorderedPair<String> newEdgeConcepts = new UnorderedPair<String>(newEdge.getSource(), newEdge.getTarget());
				if (!edgesVisited.contains(newEdgeConcepts))
					edgesToVisit.add(newEdge);
			}
		}
		return cycles;
	}

	/**
	 * returns the blended concept (if existing) containing the given text. The concept will be a blend of the form *|text OR text|*
	 * 
	 * @param blendSpace
	 * @param text
	 * @return
	 */
	public static String getBlendContainingConcept(StringGraph blendSpace, String text) {
		for (String vertex : blendSpace.getVertexSet()) {
			if (vertex.indexOf('|') < 0)
				continue;
			String[] vertexTokens = VariousUtils.fastSplit(vertex, '|');
			for (String token : vertexTokens) {
				if (token.equals(text)) {
					return vertex;
				}
			}
		}
		return null;
	}

	public static int countBlendedConcepts(StringGraph blendSpace) {
		int numberMappings = 0;
		for (String vertex : blendSpace.getVertexSet()) {
			if (vertex.indexOf('|') >= 0) {
				numberMappings++;
			}
		}
		return numberMappings;
	}

	/**
	 * Returns a new graph with the all the edges converted to facts (i.e., vertices are represented as integer constants).
	 * 
	 * @param graph
	 * @return
	 */
	public static StringGraph convertGraphToConstantGraph(StringGraph graph) {
		StringGraph ngraph = new StringGraph();
		// first: map vertices to numbers
		HashMap<String, String> varToConstant = new HashMap<String, String>(graph.numberOfVertices() * 2);
		for (String variable : graph.getVertexSet()) {
			int constantCounter = varToConstant.size();
			String constant = Integer.toString(constantCounter);
			varToConstant.put(variable, constant);
		}
		// second: copy edges from the given graph to the newgraph while renaming the vertices
		for (StringEdge edge : graph.edgeSet()) {
			ngraph.addEdge(//
					varToConstant.get(edge.getSource()), //
					varToConstant.get(edge.getTarget()), //
					edge.getLabel());
		}
		return ngraph;
	}

	/**
	 * Returns a new graph with the all the edges converted to rules (i.e., vertices are represented as variables/capitalized words).
	 * 
	 * @param graph
	 * @return
	 */
	public static StringGraph convertGraphToVariableGraph(StringGraph graph) {
		StringGraph ngraph = new StringGraph();
		// first: map vertices to numbers
		HashMap<String, String> vertexToVar = new HashMap<String, String>(graph.numberOfVertices() * 2);
		for (String variable : graph.getVertexSet()) {
			int varCounter = vertexToVar.size();
			String var = "V" + Integer.toString(varCounter);
			vertexToVar.put(variable, var);
		}
		// second: copy edges from the given graph to the newgraph while renaming the vertices
		for (StringEdge edge : graph.edgeSet()) {
			ngraph.addEdge(//
					vertexToVar.get(edge.getSource()), //
					vertexToVar.get(edge.getTarget()), //
					edge.getLabel());
		}
		return ngraph;
	}

	/**
	 * Returns the concept (source or target) which exists in the given conceptPair. Assumes that the conceptPair does not contain both source and target
	 * concepts (it will return the pair's left concept in that case) and that the edge does not self-connect only one of the pair's concepts.
	 * 
	 * @param conceptPair
	 * @param stringEdge
	 * @return
	 */
	public static String getConceptExistingInConceptPair(ConceptPair<String> conceptPair, StringEdge stringEdge) {
		String left = conceptPair.getLeftConcept();
		if (stringEdge.containsConcept(left)) {
			return left;
		}
		String right = conceptPair.getRightConcept();
		if (stringEdge.containsConcept(right)) {
			return right;
		}
		return null;
	}

	/**
	 * creates mirrored (reversed direction) copies of existing edges with the given labels
	 * 
	 * @param graph
	 * @param undirectedrelations
	 */
	public static void addMirroredCopyEdges(StringGraph graph, Set<String> labels) {
		for (String label : labels) {
			addMirroredCopyEdges(graph, label);
		}
	}

	public static void addMirroredCopyEdges(StringGraph graph, String label) {
		Set<StringEdge> existingEdges = graph.edgeSet(label);
		ArrayList<StringEdge> edgesToAdd = new ArrayList<StringEdge>(existingEdges.size());
		for (StringEdge edge : existingEdges) {
			StringEdge reverse = edge.reverse();
			edgesToAdd.add(reverse);
		}
//		graph.addEdge();
		graph.addEdges(edgesToAdd);
	}

	// TODO: this code is calculating wrong (higher than expected) distances.
	// start from v0 and try to reach v1
	@Deprecated
	public static int distanceBetweenVertices(StringGraph graph, String v0, String v1, int maximumDistance) {

		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		openSet.addLast(v0);

		Object2IntOpenHashMap<String> hops = new Object2IntOpenHashMap<>();
		hops.defaultReturnValue(Integer.MAX_VALUE);
		hops.put(v0, 0);

		while (!openSet.isEmpty()) {
			// get next vertex
			String currentVertex = openSet.removeFirst();

			int curHop = hops.getInt(currentVertex);

			// if we arrived at v1, abort expansion
			if (currentVertex.equals(v1)) {
				return curHop;
			}

			if (curHop == maximumDistance)
				continue;

			// expand a vertex not in the closed set
			if (!closedSet.contains(currentVertex)) {

				int nextHop = curHop + 1;

				Set<StringEdge> edges = graph.edgesOf(currentVertex);
				for (StringEdge edge : edges) {
					String oppositeConcept = edge.getOppositeOf(currentVertex);
					if (closedSet.contains(oppositeConcept))
						continue;
					// put the neighbors in the open set
					openSet.addLast(oppositeConcept);

					hops.put(oppositeConcept, nextHop);
				}

				// vertex from the open set explored, remove it from further exploration
				closedSet.add(currentVertex);
			}
		}

		// did not reach v1
		return Integer.MAX_VALUE;
	}

	/**
	 * Iterative post order traversal of the given graph starting at the given root. Skeleton code.
	 * 
	 * @param graph
	 * @param root
	 */
	public static void postOrder(StringGraph graph, String root) {
		HashSet<String> closedSet = new HashSet<String>();
		ArrayDeque<String> stack = new ArrayDeque<String>();
		ArrayDeque<String> output = new ArrayDeque<String>();
		stack.push(root);
		while (!stack.isEmpty()) {
			String node = stack.pop();
			closedSet.add(node);
			output.push(node);
			Set<String> neighborhood = graph.getNeighborVertices(node);
			for (String neighbor : neighborhood) {
				if (!closedSet.contains(neighbor)) {
					stack.push(neighbor);
				}
			}
		}
		while (!output.isEmpty()) {
			String node = output.pop();
			System.out.println(node);
		}

	}

	public static Object2IntOpenHashMap<String> countNumberOfChildrenPerSubTree(StringGraph graph, String root) {
		HashMap<String, String> cameFrom = new HashMap<>();
		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> stack = new ArrayDeque<>();
		ArrayDeque<String> output = new ArrayDeque<>();
		stack.push(root);
		while (!stack.isEmpty()) {
			String node = stack.pop();
			closedSet.add(node);
			output.push(node);
			Set<String> neighborhood = graph.getNeighborVertices(node);
			for (String neighbor : neighborhood) {
				if (!closedSet.contains(neighbor)) {
					stack.push(neighbor);
					cameFrom.put(neighbor, node);
				}
			}
		}
		Object2IntOpenHashMap<String> numChildren = new Object2IntOpenHashMap<>(output.size() * 2);
		while (!output.isEmpty()) {
			String node = output.pop();
			// children of this node is its neighborhood except ancestor
			Set<String> children = graph.getNeighborVertices(node);
			String ancestor = cameFrom.get(node);
			children.remove(ancestor);
			int childCount = 0;
			for (String child : children) {
				// add number of children for each "child" including self
				childCount += numChildren.getInt(child) + 1;
			}
			numChildren.put(node, childCount);
		}
		return numChildren;
	}

}
