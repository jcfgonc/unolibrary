package structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import utils.VariousUtils;

/**
 * A simple Custom (tab, csv or any other character) Separated Values reader.
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class CSVReader {
	public static CSVReader readCSV(String columnSeparator, File file, boolean fileHasHeader) throws IOException {
		CSVReader c = new CSVReader(columnSeparator, file, fileHasHeader);
		c.read();
		c.close();
		return c;
	}

	public static CSVReader readCSV(String columnSeparator, String filename, boolean fileHasHeader) throws IOException {
		return readCSV(columnSeparator, new File(filename), fileHasHeader);
	}

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

	public ArrayList<String> getHeader() throws IOException {
		read();
		return header;
	}

	/**
	 * calls getNumberOfColumns(0)
	 * 
	 * @return
	 */
	public int getNumberOfColumns() {
		return getNumberOfColumns(0);
	}

	public int getNumberOfColumns(int row) {
		return rows.get(row).size();
	}

	/**
	 * returns the number of rows, excluding the header row
	 * 
	 * @return
	 */
	public int getNumberOfRows() {
		return rows.size();
	}

	public ArrayList<String> getRow(int row) {
		return rows.get(row);
	}

	/**
	 * reads the file and returns the rows
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<ArrayList<String>> getRows() throws IOException {
		read();
		return rows;
	}

	private void read() throws IOException {
		if (dataRead) {
			return;
		}

		this.rows = new ArrayList<ArrayList<String>>();
		this.header = new ArrayList<>();
		boolean headRead = false;
		BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8), 1 << 24);
		String line;
		while ((line = br.readLine()) != null) {
			line = line.strip();
			String[] cells = VariousUtils.fastSplit(line, columnSeparator);
			ArrayList<String> asList = VariousUtils.arrayToArrayList(cells);
			if (fileHasHeader && !headRead) {
				this.header = asList;
				headRead = true;
			} else {
				this.rows.add(asList);
			}
		}
		br.close();
		dataRead = true;
	}

}
