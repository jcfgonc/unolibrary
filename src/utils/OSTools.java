package utils;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OSTools {
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static int numberOfCores;
	private static int numberOfSockets;
	private static int numberOfLogicalProcessors;
	private static boolean initialized = false;

	/**
	 * jcfgonc - This only works for Windowz... adapted from some post in stackoverflow, don't remember which
	 * 
	 * @return
	 */
	private static void getCPU_Info() {
		if (initialized)
			return;

		if (!isWindows()) {
			System.err.println("This can only be run on M$ Windows.");
			System.exit(-1);
		}
		String command = "cmd /C WMIC CPU Get /Format:List";
		Process process = null;
		System.out.print("querying OS for CPU info...");
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}

		numberOfCores = 0;
		numberOfSockets = 0;
		numberOfLogicalProcessors = 0;

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				// WINDOWZEEEEEEEE
				if (line.contains("NumberOfCores")) {
					numberOfCores += Integer.parseInt(line.split("=")[1]);
				}
				if (line.contains("SocketDesignation")) {
					numberOfSockets++;
				}
				if (line.contains("NumberOfLogicalProcessors")) {
					numberOfLogicalProcessors += Integer.parseInt(line.split("=")[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		initialized = true;
		System.out.println(" done.");
	}

	public static int getNumberOfCores() {
		return Runtime.getRuntime().availableProcessors() / 2;
//		getCPU_Info();
//		return numberOfCores;
	}

	public static int getNumberOfSockets() {
		getCPU_Info();
		return numberOfSockets;
	}

	public static int getNumberOfLogicalProcessors() {
		getCPU_Info();
		return numberOfLogicalProcessors;
	}

	public static boolean hasHyperThreading() throws NumberFormatException, IOException {
		getCPU_Info();
		boolean ht = numberOfLogicalProcessors > numberOfCores;
		return ht;
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

	/**
	 * Only works for windowze as it is my OS and I don't know how to convert to other OSes.
	 * 
	 * @return
	 */
	public static double getScreenScale() {
		if (isWindows()) {
			double dpi = (double) getDPI();
			double defaultDPI = 96;
			double scale = dpi / defaultDPI;
			return scale;
		} else
			return 1; // yeah...
	}
}