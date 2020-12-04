package visual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import graph.StringEdge;
import graph.StringGraph;

public class GraphStreamUtils {

	private static String CSS = "css/graph.css";

	public static MultiGraph initializeGraphStream() {
		MultiGraph visualGraph = new MultiGraph("output space");

		String styleSheet = null;
		try {
			styleSheet = new String(Files.readAllBytes(Paths.get(CSS)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		visualGraph.addAttribute("ui.stylesheet", styleSheet);
		visualGraph.addAttribute("ui.quality");
		visualGraph.addAttribute("ui.antialias");

		return visualGraph;
	}

	private static Node addNodeToVisualGraph(MultiGraph visualGraph, String nodeLabel) {
		Node node = visualGraph.getNode(nodeLabel);
		if (node == null) {
			node = visualGraph.addNode(nodeLabel);
			node.addAttribute("ui.label", nodeLabel);
			// if (nodeLabel.contains("|")) {
			// node.addAttribute("ui.class", "blend");
			// }
		}
		return node;
	}

	public static Edge addEdgeToVisualGraph(MultiGraph visualGraph, StringEdge edge) {
		String edgeSource = edge.getSource();
		String edgeTarget = edge.getTarget();
		String edgeLabel = edge.getLabel();

		Node sourceNode = addNodeToVisualGraph(visualGraph, edgeSource);
		Node targetNode = addNodeToVisualGraph(visualGraph, edgeTarget);

		String edgeID = edge.toString();
		Edge addEdge = visualGraph.getEdge(edgeID);
		if (addEdge == null) {
			addEdge = visualGraph.addEdge(edgeID, sourceNode, targetNode, true);
			addEdge.addAttribute("ui.label", edgeLabel);
//			if (Blend.isInterspaceEdge(edge)) {
//				addEdge.addAttribute("ui.class", "red");
//			}
		}
		return addEdge;
	}

	public static void addEdgesToVisualGraph(MultiGraph visualGraph, Set<StringEdge> edgesToAdd) {
		for (StringEdge edge : edgesToAdd) {
			addEdgeToVisualGraph(visualGraph, edge);
		}
	}

	public static void removeEdgesFromVisualGraph(MultiGraph visualGraph, Set<StringEdge> edgesToRemove) {
		for (StringEdge edge : edgesToRemove) {
			removeEdgeFromVisualGraph(visualGraph, edge);
		}
	}

	public static Edge removeEdgeFromVisualGraph(MultiGraph visualGraph, StringEdge edge) {
		String edgeID = edge.toString();
		Edge edgeToRemove = visualGraph.getEdge(edgeID);
		if (edgeToRemove != null) {
			visualGraph.removeEdge(edgeToRemove);

			Node n0 = visualGraph.getNode(edge.getSource());
			Node n1 = visualGraph.getNode(edge.getTarget());

			if (n0.getDegree() == 0)
				visualGraph.removeNode(n0);

			if (n1.getDegree() == 0)
				visualGraph.removeNode(n1);
		}
		return edgeToRemove;
	}

	/**
	 * detects changes between old and new string graphs and updates the multigraph with those
	 * 
	 * @param multiGraph
	 * @param oldStringGraph
	 * @param newStringGraph
	 * @return true if any change detected, false otherwise
	 */
	public static boolean detectChangesVisualGraph(MultiGraph multiGraph, StringGraph oldStringGraph, StringGraph newStringGraph) {
		// check what has been removed
		HashSet<StringEdge> removedEdges = new HashSet<StringEdge>();
		// edges were removed if they do not exist now
		for (StringEdge oldEdge : oldStringGraph.edgeSet()) {
			if (!newStringGraph.containsEdge(oldEdge)) {
				removedEdges.add(oldEdge);
			}
		}
		removeEdgesFromVisualGraph(multiGraph, removedEdges);

		// check what has been added
		HashSet<StringEdge> addedEdges = new HashSet<StringEdge>();
		// edges were added if they did not exist before
		for (StringEdge newEdge : newStringGraph.edgeSet()) {
			if (!oldStringGraph.containsEdge(newEdge)) {
				addedEdges.add(newEdge);
			}
		}
		addEdgesToVisualGraph(multiGraph, addedEdges);

		return !addedEdges.isEmpty() || !removedEdges.isEmpty();
	}

}
