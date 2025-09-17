package chatbots.googleai;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import stream.ParallelConsumer;
import structures.CSVWriter;
import structures.DirectionalMapping;

public class GoogleLLM_knowledgeExtractor {

	public static void checkOntologyISA(StringGraph graph, String rootConcept, int maximumDeepness) {
		ArrayDeque<String> openSet = new ArrayDeque<String>();
		HashSet<String> openSet_Hash = new HashSet<String>();
		HashSet<String> closedSet = new HashSet<String>();
		Object2IntOpenHashMap<String> deepness = new Object2IntOpenHashMap<String>();
		deepness.put(rootConcept, 0);

		openSet.addLast(rootConcept);
		openSet_Hash.add(rootConcept);

		while (!openSet.isEmpty()) {

			String parent = openSet.remove();
			openSet_Hash.remove(parent);
			closedSet.add(parent);

			// what concepts "ISA" parent?
			Set<StringEdge> isaEdges = graph.incomingEdgesOf(parent, "isa");

			int childDeepness = deepness.getInt(parent) + 1;

			if (!isaEdges.isEmpty() && childDeepness < maximumDeepness) {

				for (StringEdge isaEdge : isaEdges) {
					String child = isaEdge.getSource();
					try {
						askLLM_If_child_isa_parent(child, parent);
					} catch (Exception e) {
						e.printStackTrace();
					}
					// test if child ISA parent
					// allow for expansion level limit
					if (!closedSet.contains(child) && !openSet_Hash.contains(child)) {
						openSet.push(child);
						openSet_Hash.add(child);
						deepness.put(child, childDeepness);
					}
				}
			}
		}
	}

	public static boolean askLLM_If_child_isa_parent(String child, String parent) throws IOException, URISyntaxException {
		String prompt = "Use american english. Answer yes or no. If there are typos or spelling errors in the question, answer no. This is the question: is %s a %s?";
		String text = String.format(prompt, child, parent);
		System.out.println(text);
		String reply = GoogleLLM_Caller.doRequest(text).toLowerCase().strip();
		System.out.println(": " + reply);
		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else {
//			System.err.println(reply);
		}
		return false;
	}

	public static void askLLM_for_parts_of(String concept) throws IOException, URISyntaxException {
		String prompt = "one per line and in american english, what are the parts of a %concept%? do not explain the parts nor their purpose";
		String text = prompt.replace("%concept%", concept);
		String reply = GoogleLLM_Caller.doRequest(text);
		System.out.println(reply);
	}

	public static String askLLM_for_correctedText(String concept) throws IOException, URISyntaxException {
		String prompt = """
				You correct text, correcting typos, spelling errors, grammatical errors and verb conjugation. You do not explain your reasoning.
				You only give the corrected text. If the text is a valid name, title or address, you return the original text.
				If you do not recognize the text or you are unable to correct it, you return the original text. You always conjugate the verbs correctly.
				You correct the text as best as possible. This is the text to correct:

				%concept%
				""";
		String text = prompt.replace("%concept%", concept);
		String reply = GoogleLLM_Caller.doRequest(text);
		reply = reply.strip();
		if (reply.endsWith(".")) {
			reply = reply.substring(0, reply.length() - 1);
		}
		reply = reply.toLowerCase();
//		System.out.printf("%s\t%s\n", concept, reply);
		return reply;
	}

	public static void correctConcepts(StringGraph inputSpace) throws IOException, URISyntaxException, InterruptedException {
		final int numThreads = 8;
		ArrayList<DirectionalMapping<String>> replacements = new ArrayList<DirectionalMapping<String>>();
		ReentrantLock lock = new ReentrantLock();
		AtomicInteger globalQueryCounter = new AtomicInteger(0);

		ArrayList<String> concepts = new ArrayList<String>(inputSpace.getVertexSet());
		System.err.printf("%d concepts to correct\n", concepts.size());

		ParallelConsumer<String> pc = new ParallelConsumer<>(numThreads);
		pc.parallelForEach(concepts, concept -> {
			try {
				String output = askLLM_for_correctedText(concept);
				DirectionalMapping<String> cp = new DirectionalMapping<String>(concept, output);
				lock.lock();
				replacements.add(cp);
				lock.unlock();
				int counter = globalQueryCounter.incrementAndGet();
				if (counter % 10 == 0) {
					System.err.println(counter);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			// System.out.println(concept);
		});
		System.out.println("waiting");
		pc.shutdown();
		System.out.println("shutdown");

		CSVWriter csv = new CSVWriter("translation.tsv", "\t");
		for (DirectionalMapping<String> replacement : replacements) {
			csv.writeCell(replacement.getSource());
			csv.writeColumnSeparator();
			csv.writeCell(replacement.getTarget());
			csv.writeNewLine();
		}
		csv.close();
	}

	public static String[] askLLM_for_type_of(String concept) throws IOException, URISyntaxException {
		// tambem pode ser "ontology speaking, %concept% is a type of? do not explain the answers"
		// String prompt = "Use american english. Do not explain your answer nor your reasoning. Give all possible answers. Ontology speaking, what are the
		// superclasses for %concept%?";
		String prompt = """
				You are a knowledge base that answers to questions made by an expert system.
				All your knowledge is in american english.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You give all possible answers. Try to be as specific as possible and do not generalize.
				Ontology speaking, what are the superclasses for %concept%?""";
		String text = prompt.replace("%concept%", concept);
		System.out.println(text);
		String reply = GoogleLLM_Caller.doRequest(text);
		// destroy google ai text format
		reply = reply.replace("* ", "");
		reply = reply.replace(", ", ",");
		reply = reply.replace("\r\n", "\n");
		reply = reply.replace("\n", ",");
		reply = reply.replace(",,", ",");
		reply = reply.replace(".", "");
		reply = reply.replace("_", " ");
		reply = reply.toLowerCase();
		System.out.println(reply);
		String[] split = reply.split(",");
		return split;
	}

	public static void askLLM_for_parts_and_function_of(String concept) throws IOException, URISyntaxException {
//		String prompt="""
//				one answer per line, regarding a %concept% what are its parts and their specific purpose?
//				give as many parts and purposes as possible.
//				give each purpose with a single word in the form of an action verb""";
//		String prompt = "one answer per line, what are the parts and their purpose of a church? answer the purpose with a single action verb";
//				"""
//				one fact per line,
//				what are the components of %concept% and their function?
//				give their function with a single action verb. Do not explain their function.
//				""";
		String prompt = """
				You are a knowledge base that answers questions made by an expert system.
				All your knowledge is in American English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities. Try to be as specific as possible and do not generalize.
				The questions made to you are about a generic entity and their constituent parts. You answer with as many parts of the entity as possible. You answer each part as a noun in the singular form.
				For each part, you answer as many purposes for that part as possible. You answer each purpose with a single action verb.
				You answer each part in one line followed by the various purposes of that part.

				What are the parts and their purpose of a %concept%?""";

		String text = prompt.replace("%concept%", concept);
		String reply = GoogleLLM_Caller.doRequest(text);
		System.out.println(reply);
		// TODO
	}

	public static void checkISA(StringGraph graph) {
		Set<StringEdge> edgeSet = graph.edgeSet("isa");
		for (StringEdge edge : edgeSet) {
			String concept = edge.getSource();
			String superclass = edge.getTarget();
			try {
				boolean valid = askLLM_If_child_isa_parent(concept, superclass);
				if (!valid) {
					// delete existing relation
					graph.removeEdge(edge);
					// add new relations
					String[] types = askLLM_for_type_of(concept);
					ArrayList<StringEdge> newEdges = new ArrayList<StringEdge>();
					for (String newType : types) {
						StringEdge newEdge = edge.replaceTarget(newType);
						System.out.println(newEdge);
						newEdges.add(newEdge);
						if (newType.equals("person") || newType.equals("human")) {
							// TODO: ask for his her job and what it was famous for
						}
					}
					graph.addEdges(newEdges);
				}
				Thread.sleep(333);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
