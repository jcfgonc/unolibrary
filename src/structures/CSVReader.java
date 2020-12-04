package structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

import utils.VariousUtils;

/**
 * A simple Custom (tab, csv or any other character) Separated Values reader.
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class CSVReader {
	private boolean fileHasHeader;
	private ArrayList<String> header;
	private ArrayList<ArrayList<String>> rows;
	private final String columnSeparator;
	private final File file;
	private boolean dataRead;

	public CSVReader(String columnSeparator, File file, boolean fileHasHeader) throws FileNotFoundException {
		this.columnSeparator = columnSeparator;
		this.file = file;
		this.fileHasHeader = fileHasHeader;
		this.dataRead = false;
		this.rows = null;
		this.header = null;
	}

	public CSVReader(String columnSeparator, String filename, boolean containsHeader) throws FileNotFoundException {
		this(columnSeparator, new File(filename), containsHeader);
	}

	public void close() throws IOException {
	}

	public void read() throws IOException {
		this.rows = new ArrayList<ArrayList<String>>();
		this.header = new ArrayList<>();
		boolean headRead = false;
		BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8), 1 << 24);
		Pattern patternMatcher = Pattern.compile("[" + columnSeparator + "]+");
		String line;
		while ((line = br.readLine()) != null) {
			String[] cells = patternMatcher.split(line, 0);
			ArrayList<String> asList = VariousUtils.arrayToArrayList(cells);
			if (fileHasHeader && !headRead) {
				this.header = asList;
				headRead = true;
			} else {
				this.rows.add(asList);
			}
		}
		br.close();
		this.dataRead = true;
	}

	public ArrayList<String> getHeader() throws IOException {
		if (!dataRead) {
			read();
		}
		return header;
	}

	/**
	 * reads the file and returns the rows
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<ArrayList<String>> getRows() throws IOException {
		if (!dataRead) {
			read();
		}
		return rows;
	}

	public static CSVReader readCSV(String columnSeparator, File file, boolean fileHasHeader) throws IOException {
		CSVReader c = new CSVReader(columnSeparator, file, fileHasHeader);
		c.read();
		c.close();
		return c;
	}

	/**
	 * returns the number of rows, excluding the header row
	 * 
	 * @return
	 */
	public int getNumberOfRows() {
		return rows.size();
	}

	public int getNumberOfColumns(int row) {
		return rows.get(row).size();
	}

	/**
	 * calls getNumberOfColumns(0)
	 * 
	 * @return
	 */
	public int getNumberOfColumns() {
		return getNumberOfColumns(0);
	}

}
