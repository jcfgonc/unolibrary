package visual;

import java.awt.Image;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class OptionFrame {
	public static String showInputDialogStringRequest(String jframe_title, Image icon, Object message, String title) {

		JFrame frame = new JFrame(jframe_title);
		if (icon != null) {
			frame.setIconImage(icon);
		}

		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		String return_value = (String) JOptionPane.showInputDialog(frame, message, title, JOptionPane.DEFAULT_OPTION, null, null, null);

		frame.dispose();
		return return_value;
	}
}
