package utils;

import java.awt.Dimension;
import java.awt.Toolkit;

public class OSTools {
	private static String OS = System.getProperty("os.name").toLowerCase();

	public static void setLowPriorityProcess() {
		try {
			// 16384 is Below Normal priority
			Runtime.getRuntime().exec(String.format("wmic process where processid=%d CALL setpriority \"16384\"", ProcessHandle.current().pid()));
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public static int getNumberOfLogicalProcessors() {
		int logicalCores = Runtime.getRuntime().availableProcessors();
		return logicalCores;
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isSolaris() {
		return (OS.indexOf("sunos") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static int getDPI() {
		return Toolkit.getDefaultToolkit().getScreenResolution();
	}

	public static Dimension getEquivalentResolutionDPI(Dimension d) {
		double k = getScreenScale();
		double w = d.getWidth() * k;
		double h = d.getHeight() * k;
		return new Dimension((int) w, (int) h);
	}

	/**
	 * Only works for windows as it is my OS and I don't know how to convert to other OSes.
	 * 
	 * @return
	 */
	public static double getScreenScale() {
		if (isWindows()) {
			double dpi = (double) getDPI();
			double defaultDPI = 96;
			double scale = dpi / defaultDPI;
			return scale;
		} else {
			System.err.println("TODO: implement getScreenScale() for a Non-Windows OS");
			return 1; // lol
		}
	}
}