package visual;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import javax.swing.border.LineBorder;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.swingViewer.DefaultView;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.DefaultMouseManager;

import graph.GraphReadWrite;
import graph.StringGraph;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.CSVReader;
import utils.VariousUtils;

public class GraphData {

	private MultiGraph multiGraph;
	private Viewer viewer;
	private DefaultView defaultView;
	private boolean selected;
	private Int2ObjectMap<String> detailsMap;
	private Object2IntMap<String> detailsHeader;
	private StringGraph stringGraph;
	private DualHashBidiMap<String, String> conceptVsVar;
	private boolean loaded;
	private String id;
	private MouseAdapter mouseAdapter;
	private HashMap<String, String> columnKey2Description;
	private Layout layout;

	/**
	 * 
	 * @param id                    - the ID of this structure
	 * @param columnKey2Description
	 * @param int2ObjectMap
	 * @param header
	 * @param graphTriplets         - the csv line to be parsed into a graph
	 * @param graphSize             - the size of the graph's screen
	 */
	public GraphData(String id, StringGraph graph, Object2IntMap<String> detailsHeader, Int2ObjectMap<String> detailsMap,
			HashMap<String, String> columnKey2Description) {
		this.id = id;
		this.stringGraph = new StringGraph(graph);
		this.detailsMap = detailsMap;
		this.detailsHeader = detailsHeader;
		this.columnKey2Description = columnKey2Description;
		this.conceptVsVar = createAlternateVertexLabels(graph);
		this.loaded = false;
		this.selected = false;
		this.multiGraph = null; // created lazily
		this.viewer = null; // created lazily
		this.defaultView = null; // created lazily
	}

	public GraphData(String id, StringGraph graph) {
		this.id = id;
		this.stringGraph = new StringGraph(graph);
		this.detailsMap = null;
		this.detailsHeader = null;
		this.columnKey2Description = null;
		this.conceptVsVar = null;
		this.loaded = false;
		this.selected = false;
		this.multiGraph = null; // created lazily
		this.viewer = null; // created lazily
		this.defaultView = null; // created lazily
	}

	public boolean isSelected() {
		return selected;
	}

	@Override
	public String toString() {
		return id;
	}

	public String getId() {
		return id;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		if (!loaded)
			return;
		if (isSelected()) {
			multiGraph.addAttribute("ui.stylesheet", "graph { fill-color: rgba(192,224,255,128); }");
		} else {
			multiGraph.addAttribute("ui.stylesheet", "graph { fill-color: rgba(192,224,255,0); }");
		}
	}

	public MultiGraph getMultiGraph() {
		lazyLoad();
		return multiGraph;
	}

	public Viewer getViewer() {
		lazyLoad();
		return viewer;
	}

	/**
	 * this is the swing component that is to be added to a jcomponent/jpanel
	 * 
	 * @return
	 */
	public DefaultView getDefaultView() {
		lazyLoad();
		return defaultView;
	}

	private void lazyLoad() {
		if (!loaded) {
			this.multiGraph = GraphStreamUtils.initializeGraphStream();
			GraphStreamUtils.addEdgesToVisualGraph(multiGraph, stringGraph.edgeSet());

			// viewer = new Viewer(multiGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
			viewer = new Viewer(multiGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
			GraphRenderer renderer = Viewer.newGraphRenderer();
//			defaultView = (DefaultView) viewer.addDefaultView(false);
			defaultView = (DefaultView) viewer.addView(Viewer.DEFAULT_VIEW_ID, renderer, false);
			defaultView.setBorder(new LineBorder(Color.BLACK));
			layout = Layouts.newLayoutAlgorithm();
			layout.setQuality(1);
			layout.setForce(0.1);
			viewer.enableAutoLayout(layout);

			// defaultView.setName(id);
			createToolTipText();
//			defaultView.addComponentListener(new ComponentAdapter() {
//				@Override
//				public void componentShown(ComponentEvent e) {
//					super.componentShown(e);
//					defaultView.setEnabled(true);
//					defaultView.setVisible(true);
//				}
//
//				@Override
//				public void componentHidden(ComponentEvent e) {
//					// this is never called
//				}
//			});
			DefaultMouseManager manager = new DefaultMouseManager();
			defaultView.setMouseManager(manager);
			manager.release();

			// addMouseListener();
			loaded = true;
		}

	}

	public void toggleSelected() {
		lazyLoad();
		setSelected(!isSelected());
	}

	/**
	 * called externally (eg by GraphFilter) to create a bunch[] of GraphData from a CSV file
	 * 
	 */
	public static ArrayList<GraphData> createGraphsFromCSV(String columnSeparator, File file, boolean fileHasHeader,
			HashMap<String, String> columnKey2Description, boolean enableToolTips) throws IOException {
		// ----------------------------
		// load list of graphs to remove
		HashSet<StringGraph> uselessGraphs = new HashSet<>();
		File f = new File("useless graphs.txt");
		if (f.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(f));
			for (String line; (line = br.readLine()) != null;) {
				StringGraph g = GraphReadWrite.readCSVFromString(line);
				uselessGraphs.add(g);
			}
			br.close();
			System.out.printf("%d graphs to be removed\n", uselessGraphs.size());
		}
		// save graphs except the ones in the list above
		// ----------------------------

		CSVReader csvData = CSVReader.readCSV(columnSeparator, file, fileHasHeader);
		System.out.format("csv file %s loaded...\n", file);
		// int counter = 0;
		Object2IntMap<String> header = headerToMap(csvData.getHeader());
		int nGraphs = csvData.getNumberOfRows();
		GraphData[] graphs = new GraphData[nGraphs];

		ArrayList<ArrayList<String>> rows = csvData.getRows();
		IntStream.range(0, nGraphs).parallel().forEach(id -> {
			try {
				ArrayList<String> row = rows.get(id);
				String str = row.get(8);
				StringGraph g = GraphReadWrite.readCSVFromString(str);
				if (!uselessGraphs.contains(g)) {
					GraphData gd = new GraphData(Integer.toString(id), g, header, rowToMap(row), columnKey2Description);
					graphs[id] = gd;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return VariousUtils.arrayToArrayList(graphs);
	}

	private static Int2ObjectMap<String> rowToMap(ArrayList<String> row) {
		Int2ObjectArrayMap<String> map = new Int2ObjectArrayMap<>();
		for (int i = 0; i < row.size(); i++) {
			map.put(i, row.get(i));
		}
		return map;
	}

	private static Object2IntMap<String> headerToMap(ArrayList<String> row) {
		Object2IntMap<String> map = new Object2IntOpenHashMap<>();
		for (int i = 0; i < row.size(); i++) {
			map.put(row.get(i), i);
		}
		return map;
	}

	private static DualHashBidiMap<String, String> createAlternateVertexLabels(StringGraph g) {
		DualHashBidiMap<String, String> conceptVsVar = new DualHashBidiMap<>();
		Set<String> vertexSet = g.getVertexSet();
		int varCounter = 0;
		for (String concept : vertexSet) {
			String varName = "X" + varCounter;
			conceptVsVar.put(concept, varName);
			varCounter++;
		}
		return conceptVsVar;
	}

	@Override
	public int hashCode() {
		if (id == null)
			return 0;
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GraphData other = (GraphData) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	private void createToolTipText() {
		if (detailsHeader == null)
			return;
		if (detailsMap == null)
			return;
		if (columnKey2Description == null)
			return;

		// n:time n:relationTypes n:relationTypesStd n:cycles n:patternEdges n:patternVertices n:matches s:query s:pattern s:conceptVarMap s:hash
		String text = "<html>";
		for (String column : sortColumnsAscendingDescription(detailsHeader.keySet(), columnKey2Description)) {
			String columnDescription = columnKey2Description.get(column);
			if (columnDescription == null)
				continue;
			int columnId = detailsHeader.getInt(column);
			String value = detailsMap.get(columnId);
			text += String.format("%s:\t%s<br>", columnDescription, value);
		}
		defaultView.setToolTipText(text);
	}

	private ArrayList<String> sortColumnsAscendingDescription(Collection<String> columnIds, HashMap<String, String> col2Description) {
		ArrayList<String> c = new ArrayList<>(columnIds);
		c.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				String d1 = col2Description.get(o1);
				String d2 = col2Description.get(o2);
				if (d1 == null && d2 == null) {
					return 0;
				}
				if (d1 == null)
					return -1;
				if (d2 == null)
					return 1;
				return d1.compareTo(d2);
			}
		});
		return c;
	}

	public String getDetails(String column) {
		int columnId = detailsHeader.getInt(column);
		String value = detailsMap.get(columnId);
		return value;
	}

	public Object2IntMap<String> getDetailsHeader() {
		return detailsHeader;
	}

	public void changeGraphVertexLabelling(boolean alternativeLabelling) {
		if (conceptVsVar == null || conceptVsVar.isEmpty())
			return;
		Collection<Node> nodeSet = multiGraph.getNodeSet();
		for (Node vertex : nodeSet) {
			String vertexId = vertex.getId();
			String label;
			if (alternativeLabelling) {
				label = conceptVsVar.get(vertexId);
			} else {
				label = vertexId;
			}
			vertex.addAttribute("ui.label", label);
		}
	}

	private void addMouseListener() {
		if (defaultView == null)
			return;
		defaultView.addMouseListener(mouseAdapter);
	}

	public void saveGraphCSV(String filename) throws IOException {
		GraphReadWrite.writeCSV(filename, stringGraph);
	}

	public void setMouseListener(MouseAdapter ma) {
		if (mouseAdapter != null) {
			defaultView.removeMouseListener(mouseAdapter);
		}
		this.mouseAdapter = ma;
		addMouseListener();
	}

	/**
	 * use this or check this (and remember this code) to add this GraphData's graph to a gui
	 * 
	 * @param guiContainer
	 */
	public void addToGUI(Container guiContainer) {
		guiContainer.add(getDefaultView());
	}

	public void updateGraph(StringGraph newStringGraph) {
		StringGraph g = new StringGraph(newStringGraph);
		boolean changed = GraphStreamUtils.detectChangesVisualGraph(multiGraph, stringGraph, g);
		if (changed) {
			getLayout().shake();
		}

		this.stringGraph = g;
	}

	public Layout getLayout() {
		return layout;
	}

	public StringGraph getStringGraph() {
		return stringGraph;
	}

}
