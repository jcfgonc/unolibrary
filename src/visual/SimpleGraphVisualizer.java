package visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import graph.StringGraph;
import utils.VariousUtils;

/**
 * Simple Graph Visualizer JFrame used by the InteractiveExecutor (MOEA part)
 * 
 * @author jcfgonc
 *
 */
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
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							panel.resetView();
						}
					});
				} else if (key == KeyEvent.VK_K) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							panel.shakeLayout();
						}
					});
				} else if (key == KeyEvent.VK_S) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String filename = VariousUtils.generateCurrentDateAndTimeStamp() + ".tgf";
							try {
								panel.saveCurrentGraphToFile(filename);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				} else if (key == KeyEvent.VK_ESCAPE) {
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
