package visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import org.graphstream.ui.swing_viewer.DefaultView;

import graph.StringGraph;

public class GraphJPanel extends JPanel {

	private static final long serialVersionUID = 812935537252268077L;
	private DefaultView defaultView;
	private VisualGraph visualGraph;
	private List<String> valueSet;
	private List<String> keySet;
	private int currentGeneration;
	private int ndsSize;

	GraphJPanel() {
		visualGraph = new VisualGraph(0);
		defaultView = visualGraph.getDefaultView();
		setLayout(new BorderLayout());
		add(defaultView, BorderLayout.CENTER);
	}

//	@Override
//	protected void paintComponent(Graphics g) {
//		super.paintComponent(g);
//		defaultView.repaint();
//
//		Graphics2D graphics = (Graphics2D) g;
//		// graphics.setFont(LEFONT);
//		int fontSize = this.getFont().getSize();
//		int xpos = 16;
//		int ypos = 48;
//
//		graphics.setColor(Color.RED);
//		graphics.drawString("Source Data", xpos, ypos);
//		ypos += fontSize;
//
//		graphics.setColor(Color.BLACK);
//		graphics.drawString("Generation: " + 0, xpos, ypos);
//		ypos += fontSize;
//		graphics.drawString("Non Dominated Set size: " + 0, xpos, ypos);
//		ypos += fontSize;
//
//		ypos += 1 * fontSize;
//
//		graphics.setColor(Color.RED);
//		graphics.drawString("Graph Details", xpos, ypos);
//		ypos += fontSize;
//
//		if (valueSet != null && keySet != null) {
//			graphics.setColor(Color.BLACK);
//			Iterator<String> values = valueSet.iterator();
//			for (String scoreKey : keySet) {
//				String value = values.next();
//				graphics.drawString(scoreKey + ": " + value, xpos, ypos);
//				ypos += fontSize;
//			}
//		}
//	}

	public VisualGraph getVisualGraph() {
		return visualGraph;
	}

	public DefaultView getDefaultView() {
		return defaultView;
	}

	public void setKeySet(List<String> keySet) {
		this.keySet = keySet;
	}

	public void setValueSet(List<String> valueSet) {
		this.valueSet = valueSet;
	}

	public void setCurrentGeneration(int currentGeneration) {
		this.currentGeneration = currentGeneration;
	}

	public void setNDSSize(int ndsSize) {
		this.ndsSize = ndsSize;
	}

	public void refreshGraph(StringGraph graph) {
		visualGraph.refreshGraph(graph);
	}

	public void resetView() {
		visualGraph.resetView();
	}

	public void shakeLayout() {
		visualGraph.shakeLayout();
	}
}
