package graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import structures.ObjectIndex;
import utils.NonblockingBufferedReader;
import utils.VariousUtils;

public class GraphReadWrite {

	private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
	private static final Charset CHARSET_Windows_1252 = Charset.forName("Windows-1252");

	/**
	 * extracts the remaining string in the tokenizer, if existing. Else returns the ID as a string.
	 *
	 * @param st
	 * @param tokenID
	 * @return
	 */
	private static String getTGFlineLabel(StringTokenizer st) {
		String label = "";
		if (st.hasMoreTokens()) {

			while (st.hasMoreTokens()) {
				label += st.nextToken();
				if (st.hasMoreTokens())
					label += " ";
			}
		}
		return label;
	}

	public static int indexOf(char cref, char[] charBuffer, int from, int to) {
		for (int i = from; i < to; i++) {
			char c = charBuffer[i];
			if (cref == c) {
				return i;
			}
		}
		return -1;
	}

	public static ArrayList<String> loadConceptsFromFile(String filename) throws FileNotFoundException, IOException {
		ArrayList<String> concepts = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(filename, StandardCharsets.UTF_8));
		while (br.ready()) {
			String line = br.readLine().trim();
			if (!line.isEmpty()) {
				concepts.add(line);
			}
		}
		br.close();
		return concepts;
	}

	public static ArrayList<StringEdge> loadEdgesFromFile(String filename) throws FileNotFoundException, IOException {
		ArrayList<StringEdge> edges = new ArrayList<>();

		File file = new File(filename);
		FileReader fr = new FileReader(file, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(fr);
		while (br.ready()) {
			String line = br.readLine();
			if (line == null)
				break;
			// ignore empty lines
			if (line.length() == 0)
				continue;

			byte ptext[] = line.getBytes(CHARSET_Windows_1252);
			String lineConverted = new String(ptext, CHARSET_UTF_8);

			String[] tokens = VariousUtils.fastSplit(lineConverted, ',');
			int ntokens = tokens.length;
			if (ntokens < 3)
				continue;
			String sourceLabel = tokens[0];
			String edgeLabel = tokens[1];
			String targetLabel = tokens[2];

			StringEdge edge = new StringEdge(sourceLabel, targetLabel, edgeLabel);

			edges.add(edge);
		}
		br.close();
		fr.close();
		return edges;
	}

	public static StringGraph readAutoDetect(String filename, StringGraph graph) throws IOException {
		String extension = filename.substring(filename.lastIndexOf(".") + 1);
		if (extension.equalsIgnoreCase("csv")) {
			readCSV(filename, graph);
		} else if (extension.equalsIgnoreCase("dt")) {
			readDivago(filename, graph);
		} else if (extension.equalsIgnoreCase("tgf")) {
			readTGF(filename, graph);
		} else if (extension.equalsIgnoreCase("pro") || extension.equalsIgnoreCase("pl")) {
			readProlog(filename, graph);
		} else {
			System.err.println("unknown file extension: " + extension);
			System.exit(-1);
		}
		return graph;
	}

	public static StringGraph readAutoDetect(String filename) throws IOException {
		StringGraph graph = new StringGraph();
		readAutoDetect(filename, graph);
		return graph;
	}

	public static void readCSV(String filename, StringGraph graph) throws IOException, NoSuchFileException {
		BufferedReader br = Files.newBufferedReader(Paths.get(filename));
		readCSV(br, graph);
		br.close();
	}

	public static void readCSV(NonblockingBufferedReader br, StringGraph graph) throws IOException, NoSuchFileException {
		String line;
		while ((line = br.readLine()) != null) {
//			// ignore empty lines
//			line = line.trim();
//			if (line.length() == 0)
//				continue;
//			// comment lines start with #
//			if (line.indexOf('#') == 0)
//				continue;

			String[] tokens = VariousUtils.fastSplit(line, ',');
			int ntokens = tokens.length;
			if (ntokens != 3)
				continue;
			String sourceLabel = tokens[0];
			String edgeLabel = tokens[1];
			String targetLabel = tokens[2];

			graph.addEdge(sourceLabel, targetLabel, edgeLabel);
		}
	}

	public static void readCSV(BufferedReader br, StringGraph graph) throws IOException, NoSuchFileException {
		while (br.ready()) {
			String line = br.readLine().trim();
//			if (line == null)
//				break;
//			// ignore empty lines
//			line = line.trim();
//			if (line.length() == 0)
//				continue;
//			// comment lines start with #
//			if (line.indexOf('#') == 0)
//				continue;

			String[] tokens = VariousUtils.fastSplit(line, ',');
			int ntokens = tokens.length;
			if (ntokens != 3)
				continue;
			String sourceLabel = tokens[0];
			String edgeLabel = tokens[1];
			String targetLabel = tokens[2];

			graph.addEdge(sourceLabel, targetLabel, edgeLabel);
		}
	}

	/**
	 * creates a StringGraph from a text description of the form source0,label0,target0;source1,label1,target1;...;
	 * 
	 * @param string
	 * @return
	 * @throws NoSuchFileException
	 * @throws IOException
	 */
	public static StringGraph readCSVFromString(String string) throws NoSuchFileException, IOException {
		StringGraph graph = new StringGraph();
		//
		String[] edges_s = VariousUtils.fastSplit(string, ';');
		for (String edge_s : edges_s) {
			if (edge_s.isEmpty())
				continue;
			String[] tokens = VariousUtils.fastSplit(edge_s, ',');
			int ntokens = tokens.length;
			if (ntokens == 3) {
				graph.addEdge(tokens[0], tokens[2], tokens[1]);
			} else if (ntokens == 2) {
				graph.addEdge(tokens[0], tokens[1], "");
			}
		}
		return graph;
	}

	public static void readCSV(String filename, IntDirectedMultiGraph graph, ObjectIndex<String> vertexLabels, ObjectIndex<String> relationLabels)
			throws IOException {
		BufferedReader br = Files.newBufferedReader(Paths.get(filename));
		readCSV(br, graph, vertexLabels, relationLabels);
		br.close();
	}

	public static void readCSV(BufferedReader br, IntDirectedMultiGraph graph, ObjectIndex<String> vertexLabels, ObjectIndex<String> relationLabels)
			throws IOException {
		// int lineCounter = 0;

		while (br.ready()) {
			String line = br.readLine();
			// lineCounter++;
			if (line == null)
				break;
			// ignore empty lines
			if (line.length() == 0)
				continue;
			// comment lines start with #
			if (line.indexOf('#') == 0)
				continue;

			byte ptext[] = line.getBytes(CHARSET_Windows_1252);
			String lineConverted = new String(ptext, CHARSET_UTF_8);

			String[] tokens = VariousUtils.fastSplit(lineConverted, ',');
			int ntokens = tokens.length;
			if (ntokens < 3)
				continue;
			String sourceLabel = tokens[0];
			String edgeLabel = tokens[1];
			String targetLabel = tokens[2];

			int sourceId = vertexLabels.addObject(sourceLabel);
			int targetId = vertexLabels.addObject(targetLabel);
			int relationId = relationLabels.addObject(edgeLabel);

			graph.addEdge(sourceId, targetId, relationId);
		}
	}

	/**
	 * PS nao sei se isto suporta UTF8
	 * 
	 * @param filename
	 * @param graph
	 * @throws IOException
	 * 
	 */
	@Deprecated
	public static void readCSV_highPerformance(String filename, StringGraph graph) throws IOException {
		System.err.println("CUIDADO COM OS ENCODINGS...");

		RandomAccessFile aFile = new RandomAccessFile(filename, "r");
		FileChannel inChannel = aFile.getChannel();
		MappedByteBuffer byteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
		byteBuffer.load();

		char[] charBuffer = CHARSET_UTF_8.decode(byteBuffer).array();
		int n = charBuffer.length;

		int state = 0;
		int i = 0;
		int j = 0;

		String source = null;
		String target = null;
		String relation = null;

		while (i < n) {
			if (state == 0) {
				j = indexOf(',', charBuffer, i, n);
				if (j < 0)
					break;
				source = new String(charBuffer, i, j - i);
				i = j + 1;
			} else if (state == 1) {
				j = indexOf(',', charBuffer, i, n);
				if (j < 0)
					break;
				relation = new String(charBuffer, i, j - i);
				i = j + 1;
			} else if (state == 2) {
				j = indexOf('\n', charBuffer, i, n);
				if (j < 0)
					break;
				if (charBuffer[j - 1] == '\r') {
					target = new String(charBuffer, i, j - i - 1);
				} else {
					target = new String(charBuffer, i, j - i);
				}
				i = j + 1;

				StringEdge edge = new StringEdge(source, target, relation);
				graph.addEdge(edge);
			}

			state++;
			if (state > 2)
				state = 0;
		}

		byteBuffer.clear(); // do something with the data and clear/compact it.
		inChannel.close();
		aFile.close();

	}

	public static void readDivago(BufferedReader br, StringGraph graph) throws IOException {
		// :- multifile r/4, neg/4, arc/5, rule/6, frame/6, integrity/3.
		// r(bird,group,call,call).
		while (br.ready()) {
			String line = br.readLine();
			if (line == null)
				break;
			line = line.trim();
			// ignore empty lines
			if (line.length() == 0)
				continue;
			if (!line.startsWith("r("))
				continue;
			// get text after the domain and before the ending parentheses, ie,
			// (THIS)
			String cleaned = line.substring(line.indexOf(",") + 1, line.lastIndexOf(")"));
			StringTokenizer st = new StringTokenizer(cleaned, ",");

			String sourceLabel = st.nextToken().trim();
			String edgeLabel = st.nextToken().trim();
			String targetLabel = st.nextToken().trim();

			graph.addEdge(sourceLabel, targetLabel, edgeLabel);
		}
		br.close();
	}

	public static void readDivago(File file, StringGraph graph) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
		readDivago(br, graph);
		br.close();
	}

	public static void readDivago(String filename, StringGraph graph) throws IOException {
		readDivago(new File(filename), graph);
	}

	public static StringGraph readPrologFromString(String text, StringGraph graph) {
		StringReader sr = new StringReader(text);
		try {
			return readProlog(sr, graph);
		} finally {
			sr.close();
		}
	}

	public static StringGraph readProlog(Readable r, StringGraph graph) {
		Scanner sc = new Scanner(r);
		try {
			Pattern factSeparator = Pattern.compile("[\s]*\\.[\s]*");
			sc.useDelimiter(factSeparator);
			// while there are facts
			while (sc.hasNext()) {
				String fact = sc.next().trim(); // of the form p(a,b)

				String relation = fact.substring(0, fact.indexOf('(')).trim();
				String source = fact.substring(fact.indexOf('(') + 1, fact.indexOf(',')).trim();
				String target = fact.substring(fact.indexOf(',') + 1, fact.indexOf(')')).trim();

				graph.addEdge(source, target, relation);
			}
			return graph;
		} finally {
			sc.close();
		}
	}

	public static StringGraph readProlog(File file, StringGraph graph) throws IOException {
		FileReader fr = new FileReader(file, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(fr);
		try {
			return readProlog(br, graph);
		} finally {
			br.close();
		}
	}

	public static StringGraph readProlog(String filename, StringGraph graph) throws IOException {
		return readProlog(new File(filename), graph);
	}

	public static void readTGF(BufferedReader br, StringGraph graph) throws IOException {
		// because TGF uses indices to reference vertice labels in the edges
		HashMap<String, String> nodeLabels = new HashMap<>();
		boolean gettingnodes = true;
		while (br.ready()) {
			String line = br.readLine();
			if (line == null)
				break;
			line = line.trim();
			// ignore empty lines
			if (line.length() == 0)
				continue;
			// check if getting node information or edge information
			if (line.startsWith("#")) {
				// now we toggle to getting edges
				gettingnodes = false;
			} else {
				// break line into tokens
				StringTokenizer st = new StringTokenizer(line, "\t ");
				// got a node
				if (gettingnodes) {
					String token = st.nextToken();
					String nodeID = token;
					String nodeLabel = getTGFlineLabel(st);
					nodeLabels.put(nodeID, nodeLabel);
				} // got an edge
				else {
					if (!st.hasMoreTokens())
						break;
					String sourceID = st.nextToken();
					String destinationID = st.nextToken();
					String sourceLabel = nodeLabels.get(sourceID);
					String destinationLabel = nodeLabels.get(destinationID);
					// add a connection from one node to another (and vice
					// versa)
					String edgeLabel = getTGFlineLabel(st);
					graph.addEdge(new StringEdge(sourceLabel, destinationLabel, edgeLabel));
				}
			}
		}
		br.close();
	}

	public static void readTGF(File file, StringGraph graph) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
		readTGF(br, graph);
		br.close();
	}

	public static void readTGF(String filename, StringGraph graph) throws IOException {
		readTGF(new File(filename), graph);
	}

	private static String removeComma(String label) {
		String result = label.replace(",", "_"); // as specified by prof.
													// amilcar
		result = result.replace(" ", "_"); // in case the comma wasn't removed
											// previously
		result = result.replaceAll("_+", "_"); // remove multiple underscore
		return result;
	}

	public static <V, E> void writeCSV(BufferedWriter out, DirectedMultiGraph<V, E> graph) throws IOException {
		for (GraphEdge<V, E> se : graph.edgeSet()) {
			V source = se.getSource();
			V target = se.getTarget();
			E edgeLabel = se.getLabel();

			out.write(source.toString());
			out.write(",");
			out.write(edgeLabel.toString());
			out.write(",");
			out.write(target.toString());
			out.newLine();
		}
		out.flush();
	}

	public static <V, E> void writeCSV(String filename, DirectedMultiGraph<V, E> graph) throws IOException {
		BufferedWriter out = Files.newBufferedWriter(Paths.get(filename));
		writeCSV(out, graph);
		out.close();
	}

	public static void writeCSV(BufferedWriter bw, StringGraph graph) throws IOException {
		for (StringEdge se : graph.edgeSet()) {
			String source = se.getSource();
			String target = se.getTarget();
			String edgeLabel = se.getLabel();

			bw.write(source);
			bw.write(",");
			bw.write(edgeLabel);
			bw.write(",");
			bw.write(target);
			bw.newLine();
		}
		bw.flush();
	}

	public static void writeCSV(String filename, StringGraph graph) throws IOException {
		BufferedWriter out = Files.newBufferedWriter(Paths.get(filename));
		writeCSV(out, graph);
		out.close();
	}

	public static void writeDT(BufferedWriter bw, StringGraph graph, String namespace) throws IOException {
		bw.write(":-multifile r/4." + System.lineSeparator());
		bw.newLine();

		Set<StringEdge> edgeSet = graph.edgeSet();
		for (StringEdge se : edgeSet) {
			String source = removeComma(se.getSource());
			String target = removeComma(se.getTarget());
			String edgeLabel = removeComma(se.getLabel());
			bw.write(String.format("r(%s,%s,%s,%s).", namespace, source, edgeLabel, target));
			bw.newLine();
		}
		bw.flush();
	}

	public static void writeDT(File file, StringGraph graph, String namespace) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
		writeDT(bw, graph, namespace);
		bw.close();
	}

	public static void writeDT(String filename, StringGraph graph, String namespace) throws IOException {
		writeDT(new File(filename), graph, namespace);
	}

	public static void writePRO(BufferedWriter bw, StringGraph graph) throws IOException {
		Set<StringEdge> edgeSet = graph.edgeSet();
		for (StringEdge se : edgeSet) {
			String source = removeComma(se.getSource());
			String target = removeComma(se.getTarget());
			String edgeLabel = removeComma(se.getLabel());
			bw.write(String.format("%s(%s,%s).", edgeLabel, source, target));
			bw.newLine();
		}
		bw.flush();
	}

	public static void writePRO(File file, StringGraph graph) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
		writePRO(bw, graph);
		bw.close();
	}

	public static void writePRO(String filename, StringGraph graph) throws IOException {
		writePRO(new File(filename), graph);
	}

	public static <V, E> void writeTGF(BufferedWriter bw, DirectedMultiGraph<V, E> graph) throws IOException {
		// store vertices
		// store edges
		// ---------- example
		// 1 bird
		// 2 solid
		// 3 mobile
		// #
		// 1 2 property
		// 1 3 property

		HashMap<V, Integer> vertexID = new HashMap<>();
		for (V vertex : graph.vertexSet()) {
			Integer existingVertexID = vertexID.get(vertex);
			if (existingVertexID == null) {
				existingVertexID = Integer.valueOf(vertexID.size() + 1);
				vertexID.put(vertex, existingVertexID);
			}
			bw.write(existingVertexID + " " + vertex.toString());
			bw.newLine();
		}

		bw.write("#");
		bw.newLine();

		for (GraphEdge<V, E> edge : graph.edgeSet()) {
			V edgeSource = edge.getSource();
			V edgeTarget = edge.getTarget();
			String edgeLabel = edge.getLabel().toString();
			Integer sourceID = vertexID.get(edgeSource);
			Integer targetID = vertexID.get(edgeTarget);

			bw.write(sourceID + " " + targetID + " " + edgeLabel);
			bw.newLine();
		}
		bw.flush();
	}

	public static void writeTGF(BufferedWriter bw, StringGraph graph) throws IOException {
		// store vertices
		// store edges
		// ---------- example
		// 1 bird
		// 2 solid
		// 3 mobile
		// #
		// 1 2 property
		// 1 3 property

		HashMap<String, Integer> vertexID = new HashMap<>();
		for (String vertex : graph.getVertexSet()) {
			Integer existingVertexID = vertexID.get(vertex);
			if (existingVertexID == null) {
				existingVertexID = Integer.valueOf(vertexID.size() + 1);
				vertexID.put(vertex, existingVertexID);
			}
			bw.write(existingVertexID + " " + vertex);
			bw.newLine();
		}

		bw.write("#");
		bw.newLine();

		for (StringEdge edge : graph.edgeSet()) {
			String edgeSource = edge.getSource();
			String edgeTarget = edge.getTarget();
			String edgeLabel = edge.getLabel();
			Integer sourceID = vertexID.get(edgeSource);
			Integer targetID = vertexID.get(edgeTarget);

			bw.write(sourceID + " " + targetID + " " + edgeLabel);
			bw.newLine();
		}
		bw.flush();
	}

	public static <V, E> void writeTGF(File file, DirectedMultiGraph<V, E> graph) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
		writeTGF(bw, graph);
		bw.close();
	}

	public static void writeTGF(File file, StringGraph graph) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
		writeTGF(bw, graph);
		bw.close();
	}

	public static <V, E> void writeTGF(String filename, DirectedMultiGraph<V, E> graph) throws IOException {
		writeTGF(new File(filename), graph);
	}

	public static void writeTGF(String filename, StringGraph graph) throws IOException {
		writeTGF(new File(filename), graph);
	}

}
