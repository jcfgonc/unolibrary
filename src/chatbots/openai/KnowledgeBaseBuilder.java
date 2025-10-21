package chatbots.openai;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import stream.ParallelConsumer;

public class KnowledgeBaseBuilder {
	public static void main(String[] args) throws IOException, InterruptedException {

		// read input space
		StringGraph inputSpace = new StringGraph();
		GraphReadWrite.readCSV("new facts v3.csv", inputSpace);

		// TODO use this
		// OpenAiLLM_Caller.correctPluralConcepts(inputSpace);

		// correctCreatedByConcepts(inputSpace);
		// GraphReadWrite.writeCSV("new facts v3.csv", inputSpace);

		// System.exit(0);

		correctConceptsWithIS(inputSpace);
		System.exit(0);

		Set<String> concepts = inputSpace.getVertexSet();
		for (String concept : concepts) {
			if (concept.startsWith("to "))
				continue;
			if (concept.indexOf('(') != -1 || concept.indexOf(')') != -1) {
				String insidep = concept.substring(concept.indexOf('(') + 1, concept.indexOf(')'));
				// System.out.println(concept+"\t"+insidep);
//				
				String remaining = concept.substring(concept.indexOf(")") + 1).strip();
				if (remaining.isBlank())
					continue;
//				System.out.println(concept);
				Set<StringEdge> edgesOf = inputSpace.edgesOf(concept);
				System.out.printf("%s\t-%s-\t%s\n", concept, insidep, remaining);
//				Set<StringEdge> edgesOf = inputSpace.edgesOf(concept);
//				System.out.println(edgesOf.size() + "\t" + edgesOf);
//			String newConcept = concept.replaceAll("\\s*\\(.+\\).*$", "");
//			conceptMap.put(concept, newConcept);
			}
		}

		// ------------
		// ------------
		// first KB building phase
//		OpenAiLLM_Caller.populateKB_withClassExamplesUsingPrompts(inputSpace, "data/classes_and_prompts.txt");

//		// process invalid concepts in the KB
//		ArrayList<String> concepts = new ArrayList<>(inputSpace.getVertexSet());
//		ConcurrentHashMap<String, String> conceptMap = new ConcurrentHashMap<>();
//		concepts.parallelStream().forEach(concept -> {
//			if (concept.indexOf('(') != -1 || concept.indexOf(')') != -1) {
//				Set<StringEdge> edgesOf = inputSpace.edgesOf(concept);
//				System.out.println(edgesOf.size() + "\t" + edgesOf);
////				String newConcept = concept.replaceAll("\\s*\\(.+\\).*$", "");
////				conceptMap.put(concept, newConcept);
//			}
//		});
//		conceptMap.entrySet().forEach(entry -> System.out.printf("%s\t%s\n", entry.getKey(), entry.getValue()));

//		// use contextualized concepts from a file
//		HashMap<String, String> mapp = VariousUtils.readTwoColumnFile("data/mais conceitos.txt");
//		ArrayList<StringEdge> toAdd = new ArrayList<>();
//		Iterator<Entry<String, String>> iterator = mapp.entrySet().iterator();
//		ArrayList<StringEdge> isaedges = new ArrayList<>();
//		while (iterator.hasNext()) {
//			Entry<String, String> next = iterator.next();
//			String value = next.getValue();
//			String key = next.getKey();
//			StringEdge edge = new StringEdge(key, value, "isa");
//			isaedges.add(edge);
//		}
//		ArrayList<StringEdge> outedges = OpenAiLLM_Caller.getAllRelationsContextualized_Concurrent(isaedges);
//		toAdd.addAll(outedges);

		// ------------
		// ------------
		// second KB building phase, use populateKB_expandFromExistingConcepts and
		// generic classes in concept_classes.txt
		// OpenAiLLM_Caller.populateKB_expandFromExistingConcepts(inputSpace);

		// ------------
		// ------------
		// individual KB treatments

		// ArrayList<String> toDelete = VariousUtils.readFileRows("concepts to delete.txt");
		// inputSpace.removeVertices(toDelete);
		// inputSpace.getVertexSet().parallelStream().forEach(vertex -> {
		// if (vertex.length() < 4) {
		// Set<StringEdge> edgesOf = inputSpace.edgesOf(vertex);
		// System.out.printf("%s\t%d\t%s\n", vertex, edgesOf.size(), edgesOf);
		// }
		// });
		// doStudy(inputSpace);

//		GraphReadWrite.writeCSV("new facts v3.csv", inputSpace);

//		System.exit(0);
	}

	private static void correctConceptsWithIS(StringGraph inputSpace) {
		// corrects stuff like:
		// spawn (comic character) is part of the hellspawn group
		// conduct disorder is described in diagnostic manuals like the dsm
		// water (cosmetic) is made of deionized water
		// the simian immunodeficiency virus (siv) from chimpanzees and sooty mangabey monkeys is the
		Set<String> concepts = inputSpace.getVertexSet();
		for (String concept : concepts) {
			if (concept.startsWith("to "))
				continue; // do nothing for now
			Set<StringEdge> concept_edges = inputSpace.edgesOf(concept);
			int num_edges = concept_edges.size();
			if (num_edges == 1) {
				boolean updateEdge = false;
				StringEdge new_edge = null;
				StringEdge edge = concept_edges.iterator().next();
				String source = edge.getSource();
				String target = edge.getTarget();
				// -------------------------------------------------------------------------
				if (concept.contains(" is part ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					// is part of
					if (target.equals(concept)) {
						// DONE
						String substring = concept.substring(concept.indexOf(" is part of ") + 12);
						ArrayList<String> extracted_concepts = checkSuchAs(substring);
						if (extracted_concepts.isEmpty()) {
							new_edge = new StringEdge(source, substring, "partof");
						} else {
							for (String new_target : extracted_concepts) {
								new_edge = new StringEdge(source, new_target, "partof");
							}
						}
						updateEdge = true;
					}
				} else if (concept.contains(" is described ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					System.lineSeparator();
				} else // -------------------------------------------------------------------------
				if (concept.contains(" is made ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					if (target.equals(concept)) {
						// DONE
						String substring = concept.substring(concept.indexOf(" made of ") + 9);
						ArrayList<String> extracted_concepts = checkSuchAs(substring);
						if (extracted_concepts.isEmpty()) {
							new_edge = new StringEdge(source, substring, "madeof");
						} else {
							for (String new_target : extracted_concepts) {
								new_edge = new StringEdge(source, new_target, "madeof");
							}
						}
						updateEdge = true;
					}
				} else // -------------------------------------------------------------------------
				if (concept.contains(" is the ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					System.lineSeparator();
				}
				if (updateEdge) {
					System.out.printf("%s\t->\t%s\n", edge, new_edge);
					inputSpace.removeEdge(edge);
					assert new_edge != null;
					inputSpace.addEdge(new_edge);
				}
			}
		}
	}

	/**
	 * checks for text " such as " and extracts the related text
	 * 
	 * @param concept
	 * @return
	 */
	public static ArrayList<String> checkSuchAs(String concept) {
		// TODO complete to support text having commas, ie
		// TODO muon is made of intrinsic properties such as electric charge; mass; and spin
		ArrayList<String> concepts = new ArrayList<String>();
		int i = concept.indexOf(" such as ");
		if (i != -1) {
			String start = concept.substring(0, i);
			concepts.add(start);
			String end = concept.substring(i + 9);
			// can have and, or OR nothing
			int or = end.indexOf(" or ");
			int and = end.indexOf(" and ");
			if (or != -1) {
				// not adding first because c0 might contain multiple concepts
				// String c0 = end.substring(0, or);
				String c1 = end.substring(or + 4);
				// concepts.add(c0);
				concepts.add(c1);
			} else if (and != -1) {
				// OK
				// not adding first because c0 might contain multiple concepts
				// String c0 = end.substring(0, and);
				String c1 = end.substring(and + 5);
				// concepts.add(c0);
				concepts.add(c1);
			} else {
				System.lineSeparator();

			}
		}
		return concepts;
	}

	public static void correctCreatedByConcepts(StringGraph inputSpace) {
		// corrects stuff like
		// meteorite can create meteorite fragments,createdby,meteorite
		// comic character can create fan art,createdby,comic character
		HashMap<String, String> conversion = new HashMap<String, String>();
		HashSet<String> toDelete = new HashSet<String>();
		for (StringEdge edge : inputSpace.edgeSet("createdby")) {
			// ---------
			String source = edge.getSource();
			if (source.contains(" not created by ")) {
				System.err.println("TODO1");
			}
			if (source.contains(" does not ") || source.contains(" do not ")) {
				// OK
				toDelete.add(source);
			}
			if (source.contains(" create ")) {
				// OK
				String nsource = source.substring(source.indexOf(" create ") + 8);
				conversion.put(source, nsource);
				toDelete.add(source);
			} else if (source.contains(" creates ")) {
				// OK
				String nsource = source.substring(source.indexOf(" creates ") + 9);
				conversion.put(source, nsource);
				toDelete.add(source);
			} else if (source.contains(" created by ")) {
				if (source.startsWith("no ") || source.contains(" not created by ")) {
					// OK simply delete
					toDelete.add(source);
				}
			} else if (source.contains(" created the ")) {
				String nsource = source.substring(source.indexOf(" creates ") + 9);
				conversion.put(source, nsource);
				toDelete.add(source);
			}
			//
			String target = edge.getTarget();
			if (target.contains("not created by ") || target.contains("no single creator") || target.contains("no creator")) {
				// OK
				toDelete.add(target);
			} else if (target.contains(" created by ")) {
				// OK
				String ntarget = target.substring(target.indexOf(" created by ") + 12);
				conversion.put(target, ntarget);
				toDelete.add(target);
			} else if (target.contains(" create ")) {
				// complicated
			} else if (target.contains(" created ")) {
				// OK
				String ntarget = target.substring(0, target.indexOf(" created ") + 0);
				conversion.put(target, ntarget);
				toDelete.add(target);
			}
		}
		conversion.entrySet().forEach(entry -> inputSpace.renameVertex(entry.getKey(), entry.getValue()));
		toDelete.forEach(concept -> inputSpace.removeVertex(concept));
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
