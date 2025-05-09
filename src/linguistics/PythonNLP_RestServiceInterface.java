package linguistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import graph.GraphReadWrite;
import graph.StringGraph;
import structures.SynchronizedSeriarizableHashMap;
import utils.VariousUtils;

public class PythonNLP_RestServiceInterface {
	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedSeriarizableHashMap<String, String> cachedConstituencies = new SynchronizedSeriarizableHashMap<>("constituencyCache.dat", 15);
	private static SynchronizedSeriarizableHashMap<String, String> cachedCleaned = new SynchronizedSeriarizableHashMap<>("cleanedCache.dat", 15);
	private static HashSet<String> nounPhrases_fromFile;
	private static boolean initialized = false;
	private static final String SERVER_URL = "http://ckhp";
	private static final String NP_FILENAME = "data/noun_phrases.txt";

	public static String getConstituencyLocalHostSpacy(String concept) throws IOException {
		initialize();

		if (nounPhrases_fromFile.contains(concept))
			return "NP";

		final String baseUrl = SERVER_URL + "/api/constituency";

		String result = cachedConstituencies.get(concept);
		if (result == null) {
			result = getResponseForConcept(concept, baseUrl);
			cachedConstituencies.put(concept, result);
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
	 */
	public static String stripDeterminantsAndSingularize(String concept) throws MalformedURLException, ProtocolException, IOException {
		initialize();

		final String baseUrl = SERVER_URL + "/api/clean";

		String result = cachedCleaned.get(concept);
		if (result == null) {
			result = getResponseForConcept(concept, baseUrl);
			cachedCleaned.put(concept, result);
		}
		return result;
	}

	private static String getResponseForConcept(String concept, final String url) throws MalformedURLException, ProtocolException, IOException {
		initialize();
		String concept_encoded = URLEncoder.encode(concept.toLowerCase().trim(), "UTF-8");
		final String params = "concept=" + concept_encoded;
		String output_encoded = getRemoteResponse(params, url);
		//System.out.printf("%s->%s\n", concept_encoded, output_encoded);
		String output_decoded = URLDecoder.decode(output_encoded, "UTF-8");
		return output_decoded;
	}

	private static String getRemoteResponse(String params, final String baseUrl) throws MalformedURLException, IOException, ProtocolException {
		String constituency;
		URL url = new URL(baseUrl + "?" + params);
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
		constituency = response.toString().trim();
		return constituency;
	}

	private static void initialize() throws FileNotFoundException {
		if (!initialized) {
			nounPhrases_fromFile = new HashSet<String>();
			if (VariousUtils.checkIfFileExists(NP_FILENAME)) {
				ArrayList<String> rows = VariousUtils.readFileRows(NP_FILENAME);
				nounPhrases_fromFile.addAll(rows);
			}
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
		});
	}

}
