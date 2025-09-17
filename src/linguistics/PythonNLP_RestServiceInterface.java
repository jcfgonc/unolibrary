package linguistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import structures.SynchronizedSeriarizableHashMap;
import utils.VariousUtils;

public class PythonNLP_RestServiceInterface {
	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedSeriarizableHashMap<String, String> cachedConstituencies = new SynchronizedSeriarizableHashMap<>("constituencyCache.dat", 10);
	private static SynchronizedSeriarizableHashMap<String, String> cachedCleaned = new SynchronizedSeriarizableHashMap<>("cleanedCache.dat", 10);
	private static Set<String> nounPhrases_fromFile;
	private static boolean initialized = false;
	private static final String SERVER_URL = "http://localhost";
	private static final String NP_FILENAME = "data/noun_phrases.txt";

	public static String getConstituencyLocalHostSpacy(String concept) throws IOException, URISyntaxException {
		initialize();

		if (nounPhrases_fromFile.contains(concept))
			return "NP";

		final String baseUrl = SERVER_URL + "/api/constituency";

		String result = cachedConstituencies.get(concept);
		if (result == null) {
			result = getResponseForConcept(concept, baseUrl);
			cachedConstituencies.put(concept, result);
//			System.out.println(result);
		}

		return calculatePhraseType(result);
	}

	/**
	 * Converts to singular expression and removes determinants
	 * 
	 * @param strip
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public static String stripDeterminantsAndSingularize(String concept) throws MalformedURLException, ProtocolException, IOException, URISyntaxException {
		initialize();

		final String baseUrl = SERVER_URL + "/api/clean";

		String result = cachedCleaned.get(concept);
		if (result == null) {
			result = getResponseForConcept(concept, baseUrl);
			cachedCleaned.put(concept, result);
		}
		return result;
	}

	private static String getResponseForConcept(String concept, final String url)
			throws MalformedURLException, ProtocolException, IOException, URISyntaxException {
		initialize();
		String concept_encoded = URLEncoder.encode(concept.toLowerCase().strip(), "UTF-8");
		final String params = "concept=" + concept_encoded;
		String output_encoded = getRemoteResponse(params, url);
		// System.out.printf("%s->%s\n", concept_encoded, output_encoded);
		String output_decoded = URLDecoder.decode(output_encoded, "UTF-8");
		return output_decoded;
	}

	private static String getRemoteResponse(String params, final String baseUrl)
			throws MalformedURLException, IOException, ProtocolException, URISyntaxException {
		String constituency;
		URL url = new URI(baseUrl + "?" + params).toURL();
		;
//		URL url = new URL(baseUrl + "?" + params);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();
		// System.out.println("Response Code: " + connection.getResponseCode());
		constituency = response.toString().strip();
		return constituency;
	}

	private static void initialize() throws FileNotFoundException {
		if (!initialized) {
			nounPhrases_fromFile = new HashSet<String>();
			if (VariousUtils.checkIfFileExists(NP_FILENAME)) {
				ArrayList<String> rows = VariousUtils.readFileRows(NP_FILENAME);
				nounPhrases_fromFile.addAll(rows);
			}
			// set read only
			nounPhrases_fromFile = Collections.unmodifiableSet(nounPhrases_fromFile);
			initialized = true;
		}
	}

	private static String calculatePhraseType(String constituency) {
		// assumes that the argument is given in lower case
		if (constituency.contains("(V")) { // catches (S (V*
			return "VP";
		}
		if (constituency.contains("(ADVP")) { // catches (S (V*
			return constituency;
		}
		if (constituency.startsWith("(NP ")) {
			return "NP";
		}
		if (constituency.startsWith("(ADJP (FW")) {
			return "NP";
		}
		if (constituency.startsWith("(S (N")) {
			return "NP";
		}
		if (constituency.startsWith("(S (JJ")) {
			return "NP";
		}
		if (constituency.startsWith("(S (FW")) {
			return "NP";
		}
		return constituency;
	}

	public static void testPhraseType() throws FileNotFoundException, IOException, InterruptedException {
		StringGraph inputSpace1 = GraphReadWrite.readInputSpaceCSV("newfacts.csv");
		StringGraph inputSpace2 = GraphReadWrite.readInputSpaceCSV("verified.csv");

		List<String> concepts = VariousUtils.readFileRows("test concepts.txt");
		concepts.addAll(inputSpace1.getVertexSet());
		concepts.addAll(inputSpace2.getVertexSet());
//		concepts.addAll(inputSpace.getVertexSet());

		concepts.parallelStream().forEach(concept -> {
			try {
				String cons = getConstituencyLocalHostSpacy(concept);

				if (cons.startsWith("("))
					System.out.printf("%s\t%s\t%d\t%d\n", cons, concept, concept.split(" ").length, concept.length());
				else
					System.out.printf("---%s\t%s\t%d\t%d\n", cons, concept, concept.split(" ").length, concept.length());
				// System.out.printf("%s\t%s\n", concept, cons);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// System.out.printf("%s\t%s\n", concept, getPhraseType(concept));
			catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public static boolean isRelationValid(StringEdge edge) throws IOException, URISyntaxException {
		/**
		 * relations to verify: atlocation capableof causes causesdesire createdby desires isa knownfor madeof notdesires partof requires usedto
		 */
		String cons_source = getConstituencyLocalHostSpacy(edge.getSource());
		String cons_target = getConstituencyLocalHostSpacy(edge.getTarget());
		String label = edge.getLabel();
		switch (label) {
		case "atlocation": // checked
			// name atlocation name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "capableof": // checked
			// <> capableof VP
			if (cons_target.equals("VP"))
				return true;
			break;

		case "causes": // checked
			// <> causes name
			if (cons_target.equals("NP"))
				return true;
			break;

		case "causesdesire": // checked
			// name causesdesire name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "createdby": // checked
			// name createdby name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "desires": // checked
			// name desires <>
			if (cons_source.equals("NP"))
				return true;
			break;

		case "isa": // checked
			// name isa name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "knownfor": // checked
			// name knownfor <>
			if (cons_source.equals("NP"))
				return true;
			break;

		case "madeof": // checked
			// name madeof name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "notdesires": // checked
			// name notdesires <>
			if (cons_source.equals("NP"))
				return true;
			break;

		case "partof": // checked
			// name partof name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "requires": // checked
			// <> requires name
			if (cons_target.equals("NP"))
				return true;
			break;

		case "usedto": // checked
			// <> usedto verb
			if (cons_target.equals("VP"))
				return true;
			break;

		}
		return false;
	}

}
