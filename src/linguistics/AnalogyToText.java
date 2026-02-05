package linguistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import chatbots.openai.OpenAiLLM_Caller;
import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import utils.VariousUtils;

public class AnalogyToText {

	private static HashMap<String, String> REL_TEXT_TEMPLATES = null;

	public static void main(String[] args) throws NoSuchFileException, IOException, URISyntaxException {
		// analogy format I get from my excel file
		// root pair TAB graph in CSV
		String txt = "fridge|orchestra	fridge|orchestra,atlocation,south london|concert hall;fridge|orchestra,usedto,refrigeration|make music;yogurt|instrument,atlocation,fridge|orchestra;fridge|orchestra,isa,refrigerator|group of musician;fridge|orchestra,partof,kitchen|theatre;fridge|orchestra,capableof,cool food|play symphony;oven|band,antonym,fridge|orchestra;";
		textifyAnalogy(txt);
	}

	public static void textifyAnalogy(String txt) throws NoSuchFileException, IOException, URISyntaxException {
		String[] tokens = txt.split("\t");

		String startingVertex = tokens[0];
		StringGraph analogy = GraphReadWrite.readCSVFromString(tokens[1]);
		textifyAnalogy(startingVertex, analogy);
	}

	public static void textifyAnalogy(String refPair, StringGraph mapping) throws IOException {
		initializeRelationToTextTemplates();

		// remove crappy relations
		mapping.removeEdgesByLabel("synonym");
		mapping.removeEdgesByLabel("antonym");

		// generate the description of the proportional analogy
		ArrayList<StringEdge> edgeSeq = createEdgeSequenceBFS(mapping, refPair);
		ArrayList<String> sentences = textifyAnalogy(edgeSeq, REL_TEXT_TEMPLATES);

		// correct some minor issues
		// analogy_facts = analogy_facts.replace("to to ", "to ");

		String full_sentence = "";
		for (String sentence : sentences) {
			full_sentence += sentence + " ";
		}

		String corrected_sentence = prettifyAnalogy(full_sentence);
		System.out.println("---------------------------");
		System.out.println(full_sentence);
		System.out.println(corrected_sentence);

//		// prettify the analogy using a LLM
//		// get central concepts of the analogy
//		String[] concepts = refPair.split("\\|");
//		String concept_a = concepts[0];
//		String concept_b = concepts[1];
//		String prompt_template = VariousUtils.readFile("data/analogy_text_prompt_template_1.txt");
//		String prompt = prompt_template.replace("%a%", concept_a).replace("%b%", concept_b).replace("%text%", analogy_facts);
//		// String reply = GoogleLLM_Caller.doRequest(prompt);
//		String reply = OpenAiLLM_Caller.doRequest(prompt);
//
//		System.out.println(reply);
	}

	private static void initializeRelationToTextTemplates() {
		if (AnalogyToText.REL_TEXT_TEMPLATES == null) {
			String filename = "D:\\My Source Code\\Java - PhD\\MapperMO\\data\\relation_to_text_templates1.tsv";
			AnalogyToText.REL_TEXT_TEMPLATES = readRelationToTextTemplates(filename, "\t", true);
		}
	}

	public static ArrayList<String> textifyAnalogy(ArrayList<StringEdge> edgeSeq, HashMap<String, String> rt_templates) {
		ArrayList<String> sentences = new ArrayList<String>();
		for (StringEdge edge : edgeSeq) {
			String e_source = edge.getSource();
			String e_target = edge.getTarget();
			String relation = edge.getLabel();
			String[] sources = e_source.split("\\|");
			String[] targets = e_target.split("\\|");
			String a = sources[0];
			String b = targets[0];
			String c = sources[1];
			String d = targets[1];
			String template = rt_templates.get(relation);
			String sentence = template.replace("%a%", a).replace("%b%", b).replace("%c%", c).replace("%d%", d) + ".";
			sentences.add(sentence);
		}
		return sentences;
	}

	public static HashMap<String, String> readRelationToTextTemplates(String filePath, String delimiter, boolean containsHeader) {
		HashMap<String, String> templates = new HashMap<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

			if (containsHeader) {
				br.readLine();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(delimiter);
				assert tokens.length >= 2; // must have relation and template
				String relation = tokens[0];
				String template = tokens[1];
				templates.put(relation, template);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return templates;
	}

	/**
	 * Creates an edge sequence using a breadth first edge expansion starting at the given vertex. Used to generate textual descriptions of a graph.
	 * 
	 * @param graph
	 * @param startingVertex
	 * @return
	 */
	public static ArrayList<StringEdge> createEdgeSequenceBFS(StringGraph graph, String startingVertex) {
		ArrayList<StringEdge> edgeSequence = new ArrayList<StringEdge>();
		ArrayDeque<StringEdge> edgesToVisit = new ArrayDeque<StringEdge>();
		HashSet<StringEdge> visitedEdges = new HashSet<StringEdge>();
		edgesToVisit.addAll(graph.edgesOf(startingVertex));

		while (!edgesToVisit.isEmpty()) {
			StringEdge edge = edgesToVisit.remove();
			if (visitedEdges.contains(edge))
				continue;
			edgeSequence.add(edge);
			// System.out.println(edge);
			Set<StringEdge> neighboringEdges = graph.edgesOf(edge);
			for (StringEdge nextEdge : neighboringEdges) {
				if (visitedEdges.contains(nextEdge))
					continue;
				edgesToVisit.add(nextEdge);
			}
			visitedEdges.add(edge);
		}
		return edgeSequence;
	}

	public static String prettifyAnalogy(String analogyText) {
		String prompt = """
				The text at the end of this prompt is missing indefinite or definite articles. 
				The text has also an incorrect verb conjugation. Correct its verb conjugation. 
				The grammatical gender of the nouns may also be incorrect, correct the grammatical gender. Capitalize words as needed. Adjust the singular and plural of the nouns. 
				The text is creative and as such when doing the correction, do not attempt to make the text logical or meaningful. 
				Do not criticize dubious or erroneous claims in that text. The text is the following:

				%s
				""";
		String text = String.format(prompt.strip(), analogyText);
		String reply = "";

		reply = OpenAiLLM_Caller.cleanReply(OpenAiLLM_Caller.doRequest(text).strip());
		return reply;
	}
}
