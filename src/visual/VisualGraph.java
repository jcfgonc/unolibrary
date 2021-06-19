package visual;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.swing.SwingGraphRenderer;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;
import org.graphstream.ui.view.Viewer.ThreadingModel;
import org.graphstream.ui.view.camera.DefaultCamera2D;

import graph.StringGraph;

/**
 * Class containing stuff needed to render a graph using graphstream's API. Abstracts graphstream's specifics from the user who just wants to draw
 * some graphs.
 * 
 * @author jcgonc@gmail.com
 *
 */
public class VisualGraph {

	private MultiGraph multiGraph;
	private Viewer viewer;
	/**
	 * this is what is rendered/visualised in a gui container
	 */
	private DefaultView defaultView;
	/**
	 * unique id for this graph
	 */
	private int id;
	/**
	 * graph layout engine
	 */
	private Layout layout;
	private Point dragStartingPoint;
	private Point dragEndingPoint;
	private double rotationDegrees;

	public VisualGraph(int uniqueID) {
		id = uniqueID;
		rotationDegrees = 0;
		String id_str = Integer.toString(uniqueID);
		multiGraph = GraphStreamUtils.initializeGraphStream(id_str);

		// its viewer/renderer
		viewer = new SwingViewer(multiGraph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(CloseFramePolicy.CLOSE_VIEWER);

		defaultView = (DefaultView) viewer.addView("graph" + id_str, new SwingGraphRenderer(), false);
		defaultView.setBorder(new LineBorder(Color.BLACK));

		// disable graphstream's keyboard shortcuts (interfere with the GUI)
		removeListeners();

		addMotionListener();

		// and setup the layout engine
		layout = Layouts.newLayoutAlgorithm();
		viewer.enableAutoLayout(layout);
	}

	/**
	 * shake a little the vertices of the graph
	 */
	public void shakeLayout() {
		layout.shake();
	}

	/**
	 * starts the layout of the graph from a random configuration
	 */
	public void restartLayout() {
		resetView();
		viewer.disableAutoLayout();
		viewer.enableAutoLayout();
	}

	/**
	 * clears the edges/nodes of the visual graph while maintaining the style sheet
	 */
	public void clear() {
		multiGraph.clear(); // only function graphstream has to clear nodes/edges (which also clears styles)
		GraphStreamUtils.setupStyleSheet(multiGraph); // dumb graphstream clears styles, recreate them
	}

	/**
	 * clears the edges/nodes of the visual graph replacing it the the given one
	 * 
	 * @param stringGraph
	 */
	public void refreshGraph(StringGraph stringGraph) {
		clear();
		GraphStreamUtils.addEdgesToGraph(multiGraph, stringGraph.edgeSet()); // copy edges from the data-graph to the visual-graph
	}

	/**
	 * returns what is rendered/visualised in a gui container
	 * @return
	 */
	public DefaultView getDefaultView() {
		return defaultView;
	}

	public int getId() {
		return id;
	}

	public MultiGraph getMultiGraph() {
		return multiGraph;
	}

	public Viewer getViewer() {
		return viewer;
	}

	/**
	 * sets the camera's zoom (as a multiplier)
	 * 
	 * @param factor 1 is the default scale
	 */
	public void changeMagnification(double factor) {
		DefaultCamera2D camera = (DefaultCamera2D) defaultView.getCamera();
		camera.setViewPercent(factor); // dumb graphstream documentation... it's not in percent but as a factor (1=no scaling)
	}

	/**
	 * rotates the camera with the given angle
	 * 
	 * @param angleDegrees 0 does nothing
	 */
	public void changeRotation(double angleDegrees) {
		rotationDegrees += angleDegrees;
		DefaultCamera2D camera = (DefaultCamera2D) defaultView.getCamera();
		camera.setViewRotation(rotationDegrees);
	}

	/**
	 * sets the camera rotation to the given angle (in absolute, not cumulative)
	 * 
	 * @param angleDegrees 0 is the default unrotated camera
	 */
	public void changeRotationAbsolute(double angleDegrees) {
		rotationDegrees = angleDegrees;
		DefaultCamera2D camera = (DefaultCamera2D) defaultView.getCamera();
		camera.setViewRotation(rotationDegrees);
	}

	public void resetView() {
		DefaultCamera2D camera = (DefaultCamera2D) defaultView.getCamera();
		rotationDegrees = 0;
		camera.setViewRotation(rotationDegrees);
		camera.resetView();
	}

	private void mouseDraggedEvent(MouseEvent e) {
		dragEndingPoint = e.getPoint();
		if (SwingUtilities.isLeftMouseButton(e)) {
			translateGraph();
		} else if (SwingUtilities.isRightMouseButton(e)) {
			rotateGraph();
		}
		dragStartingPoint = dragEndingPoint;
	}

	private void rotateGraph() {
		Point delta = new Point(//
				dragEndingPoint.x - dragStartingPoint.x, //
				dragEndingPoint.y - dragStartingPoint.y);
		if (delta.x != 0 || delta.y != 0) {
			double angle = delta.x + delta.y;
			changeRotation(angle);
		}
	}

	private void translateGraph() {
		Point delta = new Point(//
				dragEndingPoint.x - dragStartingPoint.x, //
				dragEndingPoint.y - dragStartingPoint.y);
		if (delta.x != 0 || delta.y != 0) {
			DefaultCamera2D camera = (DefaultCamera2D) defaultView.getCamera();
			Point3 viewCenter = camera.getViewCenter();
			double scale = 0.005;
			double newX = (double) (viewCenter.x - delta.x * scale);
			double newY = (double) (viewCenter.y + delta.y * scale);
			double newZ = (double) (viewCenter.z);
			camera.setViewCenter(newX, newY, newZ);
		}
	}

	private void addMotionListener() {
		defaultView.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				// unneeded
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseDraggedEvent(e);
			}
		});
		defaultView.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// unneeded
			}

			@Override
			public void mousePressed(MouseEvent e) {
				dragStartingPoint = e.getPoint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// unneeded
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// unneeded
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// unneeded
			}
		});
	}

	/**
	 * remove damned graphstream's listeners, we don't serve their kind here
	 */
	private void removeListeners() {
		for (MouseListener listener : defaultView.getMouseListeners()) {
			defaultView.removeMouseListener(listener);
		}

		for (MouseMotionListener listener : defaultView.getMouseMotionListeners()) {
			defaultView.removeMouseMotionListener(listener);
		}

		for (KeyListener listener : defaultView.getKeyListeners()) {
			defaultView.removeKeyListener(listener);
		}
	}

	public void setToolTip(String toolTipText) {
		defaultView.setToolTipText(toolTipText);
	}
}
