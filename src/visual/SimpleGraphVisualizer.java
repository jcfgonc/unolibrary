package visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import graph.StringGraph;

public class SimpleGraphVisualizer extends JFrame {

	private static final long serialVersionUID = -1479275251183910803L;
	private GraphJPanel panel;

	public SimpleGraphVisualizer() {
		super("StringGraph Clipboard Reader");

		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "org.graphstream.ui.swingViewer.util.SwingDisplay");
		System.setProperty("org.graphstream.ui", "swing");

		panel = new GraphJPanel();
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setPreferredSize(new Dimension(320, 240));
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		initializeListeners();
	}

	private void initializeListeners() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}

		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();

				if (key == KeyEvent.VK_R) {
					panel.resetView();
				} else if (key == KeyEvent.VK_S) {
					panel.shakeLayout();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		});
	}

	public void setData(StringGraph graph, List<String> keySet, List<String> valueSet, int currentGeneration, int ndsSize) {
		if (!graph.isEmpty()) {
			panel.refreshGraph(graph);
		}

		panel.setKeySet(keySet);
		panel.setValueSet(valueSet);

		panel.setCurrentGeneration(currentGeneration);
		panel.setNDSSize(ndsSize);

		this.repaint();
	}

}
