package visual;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import graph.GraphReadWrite;
import graph.StringGraph;
import utils.VariousUtils;

public class StringGraphClipboardReader {

	public static void main(String[] args) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					createAndShowGUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	protected static void createAndShowGUI() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "org.graphstream.ui.swingViewer.util.SwingDisplay");
		System.setProperty("org.graphstream.ui", "swing");

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		VisualGraph vg = new VisualGraph(0);

		JFrame mainFrame = new JFrame("StringGraph Clipboard Reader");
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainFrame.setPreferredSize(new Dimension(320, 240));
		mainFrame.pack();
		mainFrame.setVisible(true);

		mainFrame.add(vg.getDefaultView());

		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close(mainFrame);
			}
		});

		mainFrame.addKeyListener(new KeyListener() {

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
							vg.resetView();
						}
					});
				} else if (key == KeyEvent.VK_K) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							vg.shakeLayout();
						}
					});
				} else if (key == KeyEvent.VK_S) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String filename = VariousUtils.generateCurrentDateAndTimeStamp() + ".tgf";
							try {
								vg.saveCurrentGraphToFile(filename);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				} else if (key == KeyEvent.VK_ESCAPE) {
					close(mainFrame);
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		});

		StringClipBoardListener cl = new StringClipBoardListener(clipboardText -> {
			// System.out.println(clipboardText);
			try {
				StringGraph graph = GraphReadWrite.readCSVFromString(clipboardText);
				if (!graph.isEmpty()) {
					vg.refreshGraph(graph);
//					System.out.println(graph);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		cl.start();
	}

	private static void close(JFrame mainFrame) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainFrame.dispose();
				System.exit(0);
			}
		});
	}

}
