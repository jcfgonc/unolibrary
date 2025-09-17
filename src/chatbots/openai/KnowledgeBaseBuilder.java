package chatbots.openai;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import stream.ParallelConsumer;
import utils.VariousUtils;

public class KnowledgeBaseBuilder {
	public static void main(String[] args) throws IOException, InterruptedException {

		// read input space
		StringGraph inputSpace = new StringGraph();
//	GraphReadWrite.readCSV("newfacts.csv", inputSpace);
//	GraphReadWrite.readCSV(MOEA_Config.inputSpacePath, inputSpace);

		// ------------
		// ------------
		// first KB building phase
		OpenAiLLM_Caller.populateKB_withClassExamplesUsingPrompts(inputSpace);
		
		// use contextualized concepts from a file
		HashMap<String, String> mapp = VariousUtils.readTwoColumnFile("data/mais conceitos.txt");
		ArrayList<StringEdge> toAdd = new ArrayList<>();
		Iterator<Entry<String, String>> iterator = mapp.entrySet().iterator();
		ArrayList<StringEdge> isaedges = new ArrayList<>();
		while (iterator.hasNext()) {
			Entry<String, String> next = iterator.next();
			String value = next.getValue();
			String key = next.getKey();
			StringEdge edge = new StringEdge(key, value, "isa");
			isaedges.add(edge);
		}
		ArrayList<StringEdge> outedges = OpenAiLLM_Caller.getAllRelationsContextualized_Concurrent(isaedges);
		toAdd.addAll(outedges);

		// ------------
		// ------------
		// second KB building phase, use populateKB_expandFromExistingConcepts and
		// generic classes in concept_classes.txt
//	OpenAiLLM_Caller.populateKB_expandFromExistingConcepts(inputSpace);

		// ------------
		// ------------
		// individual KB treatments

//	ArrayList<String> toDelete = VariousUtils.readFileRows("concepts to delete.txt"); 
//	inputSpace.removeVertices(toDelete);
//	inputSpace.getVertexSet().parallelStream().forEach(vertex -> {
//		if (vertex.length() < 4) {
//			Set<StringEdge> edgesOf = inputSpace.edgesOf(vertex);
//			System.out.printf("%s\t%d\t%s\n", vertex, edgesOf.size(), edgesOf);
//		}
//	});
		// doStudy(inputSpace);

		GraphReadWrite.writeCSV("new facts v3.csv", inputSpace);

		System.exit(0);
	}

	private static void doStudy(StringGraph inputSpace) throws IOException, URISyntaxException, InterruptedException {
//		ArrayList<String> initialConcepts = VariousUtils.readFileRows("newconcepts.txt");
//		HashSet<String> classes = new HashSet<>(GraphAlgorithms.getEdgesTargets(inputSpace.edgeSet("isa")));
//		for (String concept : classes) {
//			
//			// get concepts that are super classes
//			Set<StringEdge> concept_isa = inputSpace.outgoingEdgesOf(concept, "isa");
//			int degree = inputSpace.incomingEdgesOf(concept, "isa").size();
//			if (concept_isa.isEmpty()) {
//				initialConcepts.add(concept);
//				System.out.printf("%s\t%d\n", concept, degree);
//			}
//			// System.lineSeparator();
//		}

		// -------------
//		// update constituency info using a llm
//		Set<String> concepts = inputSpace.getVertexSet();
//		ArrayList<String> concepts = VariousUtils.readFileRows("D:\\Desktop\\Untitled-1.txt");
//		ParallelConsumer<String> pc = new ParallelConsumer<>();
//		pc.parallelForEach(concepts, concept -> {
//			// String spacy = PythonNLP_RestServiceInterface.getConstituencyLocalHostSpacy(concept);
//			String llm_answer = OpenAiLLM_Caller.getPhraseType(concept);
//			System.out.printf("%s\t%s\n", concept, llm_answer);
//		});
//		pc.shutdown();
//		Thread.sleep(30000);
//		OpenAiLLM_Caller.getPhraseType("this is an experiment");
//		System.exit(0);

//		System.out.println(OpenAiLLM_Caller.isRelationValid(new StringEdge("analyzing flight datum,partof,niagara falls", StringEdge.CSV_ORDER_SOURCE_LABEL_TARGET)));
//		System.out.println(OpenAiLLM_Caller.isRelationValid(new StringEdge("transistor,atlocation,circuit board", StringEdge.CSV_ORDER_SOURCE_LABEL_TARGET)));
//		System.out.println(OpenAiLLM_Caller.isRelationValid(new StringEdge("mouth,partof,paul kersey", StringEdge.CSV_ORDER_SOURCE_LABEL_TARGET)));
//		System.out.println(OpenAiLLM_Caller.isRelationValid(new StringEdge("tail,partof,axolotl", StringEdge.CSV_ORDER_SOURCE_LABEL_TARGET)));
//		System.exit(0);

//		ArrayList<StringEdge> toRemove = new ArrayList<>();
//		ArrayList<StringEdge> toAdd = new ArrayList<>();
//		for (StringEdge edge : inputSpace.edgeSet()) {
//			String label = edge.getLabel();
//			String source = edge.getSource();
//			String target = edge.getTarget();
//			// cancer creates sentimentality,createdby,cancer
//			if (!label.equals("createdby")) {
//				String text = " created ";
//				if (source.startsWith(target) && source.contains(source)) {
//					System.out.println(edge);
////					toRemove.add(edge);
////					StringEdge correctedEdge = edge.replaceSource(source.substring(source.indexOf(text) + text.length()));
////					if (!correctedEdge.getSource().isEmpty())
////						toAdd.add(correctedEdge);
////					System.out.printf("%s\t%s\n", edge, correctedEdge);
//				}
//			}
//		}
//		inputSpace.removeEdges(toRemove);
//		inputSpace.addEdges(toAdd);

//		System.exit(0);

//		OpenAiLLM_Caller.debugCaches();
//		System.exit(0);
		System.out.println(inputSpace.edgesOf("c"));

		System.exit(0);

		// correct wrong NP/VP relations (source/target incorrect phrase)
		ArrayList<StringEdge> toRemove = new ArrayList<>(1 << 24);
		ArrayList<StringEdge> toAdd = new ArrayList<>(1 << 24);
		ReentrantLock rwlock = new ReentrantLock();
		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>();
		pc.parallelForEach(inputSpace.edgeSet(), edge -> {
//			if (edge.getSource().contains("→") || edge.getTarget().contains("→")) {
//				System.out.println(edge);
//			}
//			System.out.println(concept + "\t" + OpenAiLLM_Caller.getPhraseType(concept));
			StringEdge newEdge = OpenAiLLM_Caller.correctRelation(edge);
			if (newEdge != null && !newEdge.equals(edge)) {
//				System.out.printf("WRONG:%s\n", edge);
				rwlock.lock();
				toRemove.add(edge);
				toAdd.add(newEdge);
				rwlock.unlock();
//				System.out.printf("%s\t%s\n", edge, newEdge);
			}
		});
		pc.shutdown();
		inputSpace.removeEdges(toRemove);
		inputSpace.addEdges(toAdd);
		OpenAiLLM_Caller.saveCaches();
		System.out.printf("corrected %d edges\n", toRemove.size());

		// correct relations of the form:
		// bacteria,capableof,bacteria can synthesize vitamin
		// bob marley,desires,bob marley desires peace
//		ArrayList<StringEdge> toRemove = new ArrayList<>(1 << 24);
//		ArrayList<StringEdge> toAdd = new ArrayList<>(1 << 24);
//		ReentrantLock rwlock = new ReentrantLock();
//		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>();
//		pc.parallelForEach(inputSpace.edgeSet(), edge -> {
//			String label = edge.getLabel();
//			String source = edge.getSource();
//			String target = edge.getTarget();
//			StringEdge newEdge = null;
//			if (target.startsWith(source + " ")) {
//				if (label.equals("requires")) {
//					String target_p = target.substring(target.indexOf(source) + source.length() + 1);
//					int i = VariousUtils.ORedIndexOf(target_p, "requires ", "require ");
//					if (i != -1) {
//						String target_new = target_p.substring(target_p.indexOf(' ') + 1);
//						newEdge = edge.replaceTarget(target_new);
//					}
//				} else if (label.equals("notdesires")) {
//					String target_p = target.substring(target.indexOf(source) + source.length() + 1);
//					int i = VariousUtils.ORedIndexOf(target_p, "dislikes ", "dislike ");
//					if (i != -1) {
//						String target_new = target_p.substring(target_p.indexOf(' ') + 1);
//						newEdge = edge.replaceTarget(target_new);
//					}
//				} else if (label.equals("desires")) {
//					String target_p = target.substring(target.indexOf(source) + source.length() + 1);
//					int i = VariousUtils.ORedIndexOf(target_p, "desires ", "desire ");
//					if (i != -1) {
//						String target_new = target_p.substring(target_p.indexOf(' ') + 1);
//						newEdge = edge.replaceTarget(target_new);
//					}
//				} else if (label.equals("capableof")) {
//					String target_p = target.substring(target.indexOf(source) + source.length() + 1);
//					int i = target_p.indexOf("can ");
//					if (i != -1) {
//						String target_new = target_p.substring(target_p.indexOf(' ') + 1);
//						newEdge = edge.replaceTarget(target_new);
//						// System.out.printf("%s\t%s\n", edge, newEdge);
//					}
//				} else if (label.equals("usedto") || label.equals("knownfor")) {
//					String target_p = target.substring(target.indexOf(source) + source.length() + 1);
//					newEdge = edge.replaceTarget(target_p);
//				} else {
//				}
//				if (newEdge != null) {
//					rwlock.lock();
//					toRemove.add(edge);
//					toAdd.add(newEdge);
//					rwlock.unlock();
//				}
//			}
//		});
//		pc.shutdown();
//		inputSpace.removeEdges(toRemove);
//		inputSpace.addEdges(toAdd);
//		OpenAiLLM_Caller.saveCaches();
//		System.out.printf("corrected %d edges\n", toRemove.size());

		System.exit(0);
	}

}
