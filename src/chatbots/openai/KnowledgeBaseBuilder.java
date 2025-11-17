package chatbots.openai;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import linguistics.PythonNLP_RestServiceInterface;
import stream.ParallelConsumer;
import structures.ObjectCounter;
import structures.SynchronizedSeriarizableHashMap;
import utils.VariousUtils;

public class KnowledgeBaseBuilder {
	private static final int NUMBER_OF_THREADS = 4;

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
				System.err.println("checkSuchAs TODO: " + concept);
			}
		}
		return concepts;
	}

	public static void correctLifeFormConcept(StringGraph kb) throws InterruptedException {
		HashSet<String> openSet = new HashSet<String>();
		HashSet<String> closedSet = new HashSet<String>();
		ArrayList<StringEdge> edges = new ArrayList<StringEdge>();
		edges.addAll(kb.edgeSet("desires"));
		edges.addAll(kb.edgeSet("notdesires"));

		ReentrantLock kb_lock = new ReentrantLock();
		ReentrantLock openset_lock = new ReentrantLock();
		ReentrantReadWriteLock closedset_lock = new ReentrantReadWriteLock();

		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>(8);
		pc.parallelForEach(edges, edge -> {
			String source = edge.getSource();
			boolean inClosedSet = false;
			{
				closedset_lock.readLock().lock();
				inClosedSet = closedSet.contains(source);
				closedset_lock.readLock().unlock();
			}
			if (!inClosedSet) {
				{
					closedset_lock.writeLock().lock();
					closedSet.add(source);
					closedset_lock.writeLock().unlock();
				}
				boolean is_life_form = OpenAiLLM_Caller.checkIfConceptIsLifeForm(source);
				if (is_life_form) {
					{
						openset_lock.lock();
						openSet.add(source);
						openset_lock.unlock();
					}
					StringEdge lifeform = new StringEdge(source, "life form", "isa");
					{
						kb_lock.lock();
						kb.addEdge(lifeform);
						kb_lock.unlock();
					}
					System.out.println("added " + lifeform);
				}
			}
		});
		System.out.println("removing desires/notdesires edges for " + openSet.size() + " concepts");

		for (String concept : openSet) {
			ArrayList<StringEdge> toRemove = new ArrayList<StringEdge>();
			toRemove.addAll(kb.outgoingEdgesOf(concept, "desires"));
			toRemove.addAll(kb.outgoingEdgesOf(concept, "notdesires"));
			kb.removeEdges(toRemove);
		}
	}

	public static void correctPluralConcepts(StringGraph kb) throws IOException, InterruptedException {
		HashSet<String> concepts = new HashSet<String>();
		for (StringEdge edge : kb.edgeSet("madeof")) {
			concepts.add(edge.getTarget());
		}
		for (StringEdge edge : kb.edgeSet("partof")) {
			concepts.add(edge.getSource());
		}
		HashMap<String, String> conversion = new HashMap<String, String>();
		ReentrantLock lock = new ReentrantLock();

		ParallelConsumer<String> pc = new ParallelConsumer<>(NUMBER_OF_THREADS);
		pc.parallelForEach(concepts, concept -> {
			boolean plural = OpenAiLLM_Caller.checkIfConceptIsPlural(concept);
			if (plural) {
				// get singular
				String singular = OpenAiLLM_Caller.convertPlural2Singular(concept);
				{
					lock.lock();
					conversion.put(concept, singular);
					lock.unlock();
				}
				System.out.println(concept + "\t->\t" + singular);
			}
		});

		concepts.parallelStream().forEach(concept -> {
		});
		// single thread renaming
		for (Entry<String, String> entry : conversion.entrySet()) {
			String plural = entry.getKey();
			String singular = entry.getValue();
			kb.renameVertex(plural, singular);
		}
	}

	public static void correctConceptsWithIS(StringGraph kb) {
		// corrects stuff like:
		// spawn (comic character) is part of the hellspawn group
		// conduct disorder is described in diagnostic manuals like the dsm
		// water (cosmetic) is made of deionized water
		// the simian immunodeficiency virus (siv) from chimpanzees and sooty mangabey monkeys is the
		Set<String> concepts = kb.getVertexSet();
		for (String concept : concepts) {
			if (concept.startsWith("to "))
				continue; // do nothing for now
			Set<StringEdge> concept_edges = kb.edgesOf(concept);
			int num_edges = concept_edges.size();
			if (num_edges == 1) {
				boolean updateEdge = false;
				ArrayList<StringEdge> new_edges = new ArrayList<StringEdge>();
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
							// TESTED
							new_edges.add(new StringEdge(source, substring, "partof"));
						} else {
							// UNTESTED
							for (String new_target : extracted_concepts) {
								new_edges.add(new StringEdge(source, new_target, "partof"));
							}
						}
						updateEdge = true;
					}
				} else if (concept.contains(" is described ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					System.err.printf("TODO: %s\t%s\n", concept, concept_edges);
					kb.removeEdges(concept_edges);
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
							// TESTED
							new_edges.add(new StringEdge(source, substring, "madeof"));
						} else {
							// TESTED
							for (String new_target : extracted_concepts) {
								new_edges.add(new StringEdge(source, new_target, "madeof"));
							}
						}
						updateEdge = true;
					}
				} else // -------------------------------------------------------------------------
				if (concept.contains(" is the ")) {
					if (num_edges > 1) {
						System.err.println(concept + " has " + num_edges + " edges");
					}
					System.err.printf("TODO: %s\t%s\n", concept, concept_edges);
					kb.removeEdges(concept_edges);
				}
				if (updateEdge) {
					System.out.printf("%s\t->\t%s\n", edge, new_edges.toString());
					kb.removeEdge(edge);
					assert !new_edges.isEmpty();
					kb.addEdges(new_edges);
				}
			}
		}
	}

	public static void correctConceptsWithPronouns(StringGraph kb) {
		Set<String> concepts = kb.getVertexSet();
		for (String concept : concepts) {
			String new_concept = null;
			if (concept.startsWith("the ")) {
				new_concept = concept.substring(4);
			} else if (concept.startsWith("a ")) {
				new_concept = concept.substring(2);
			} else if (concept.startsWith("an ")) {
				new_concept = concept.substring(3);
			}
			// do the replacement
			if (new_concept != null) {
				kb.renameVertex(concept, new_concept);
			}
		}
	}

	public static void correctCreatedByConcepts(StringGraph kb) {
		// corrects stuff like
		// meteorite can create meteorite fragments,createdby,meteorite
		// comic character can create fan art,createdby,comic character
		HashMap<String, String> conversion = new HashMap<String, String>();
		ArrayList<String> toDelete = new ArrayList<String>();
		for (StringEdge edge : kb.edgeSet("createdby")) {
			// verify problems in either target or source concepts
			// SOURCE
			String source = edge.getSource();
			if (source.contains(" not created by ")) {
				// TODO
				// System.err.println("TODO1 " + source);
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
			// TARGET
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
				// TODO complicated
				// System.err.println("TODO2 " + target);
			} else if (target.contains(" created ")) {
				// OK
				String ntarget = target.substring(0, target.indexOf(" created ") + 0);
				conversion.put(target, ntarget);
				toDelete.add(target);
			} else if (target.contains(" who ")) {
				int index = target.indexOf(" who ");
				String before = target.substring(0, index + 0);
//				String after = target.substring(who_ix + 5);
				ArrayList<String> parts = getAndOrComponents(before);
				for (String creator : parts) {
					StringEdge new_edge = new StringEdge(source, creator, "createdby");
					kb.addEdge(new_edge);
				}
				toDelete.add(target);
				// System.err.println(parts + "\t" + target + "\t" + before + "\t" + after);
			} else if (target.endsWith(" who")) {
				if (target.equals("guess who"))
					continue;
				int index = target.indexOf(" who");
				String before = target.substring(0, index + 0);
				ArrayList<String> parts = getAndOrComponents(before);
				for (String creator : parts) {
					StringEdge new_edge = new StringEdge(source, creator, "createdby");
					kb.addEdge(new_edge);
				}
				toDelete.add(target);
			} else if (target.contains(" such as ")) {
				// craftsmen in ancient civilizations such as mesopotamia and egypt
				int index = target.indexOf(" such as ");
				String before = target.substring(0, index + 0);
				String after = target.substring(index + 9);
				ArrayList<String> parts = getAndOrComponents(after);
				parts.add(before);
				for (String creator : parts) {
					StringEdge new_edge = new StringEdge(source, creator, "createdby");
					kb.addEdge(new_edge);
				}
				toDelete.add(target);
			}
		}
		conversion.entrySet().forEach(entry -> kb.renameVertex(entry.getKey(), entry.getValue()));
		toDelete.forEach(concept -> kb.removeVertex(concept));
	}

	public static void correctSuchAsConcepts(StringGraph kb) {
		ArrayList<StringEdge> new_edges = new ArrayList<StringEdge>();
		ArrayList<StringEdge> toRemove = new ArrayList<StringEdge>();
		for (StringEdge edge : kb.edgeSet()) {
			// TODO ver problemas no TARGET
			String source = edge.getSource();
			int suchAs_ix = source.indexOf(" such as ");
			if (suchAs_ix != -1) {
				ArrayList<String> new_concepts = new ArrayList<String>();
				if (source.contains(" or ") || source.contains(" and ")) {
					String start = source.substring(0, suchAs_ix);
					// concepts.add(start);
					String end = source.substring(suchAs_ix + 9);
					// and, or OR nothing AFTER such as
					int or = end.indexOf(" or ");
					int and = end.indexOf(" and ");
					// in general OR should be prioritized
					if (or != -1) {
						String c0 = end.substring(0, or);
						String c1 = end.substring(or + 4);
						new_concepts.add(c0);
						new_concepts.add(c1);
						new_concepts.add(start);
					} else if (and != -1) {
						String c0 = end.substring(0, and);
						String c1 = end.substring(and + 5);
						new_concepts.add(c0);
						new_concepts.add(c1);
						new_concepts.add(start);
					} else {
						// and/or is before "such as"
						// and, or OR nothing AFTER such as
						or = start.indexOf(" or ");
						and = start.indexOf(" and ");
						if (or != -1) {
							String c0 = start.substring(0, or);
							String c1 = start.substring(or + 4);
							new_concepts.add(c0);
							new_concepts.add(c1);
							new_concepts.add(end);
						} else if (and != -1) {
							// UNCHECKED
							String c0 = start.substring(0, and);
							String c1 = start.substring(and + 5);
							new_concepts.add(c0);
							new_concepts.add(c1);
							new_concepts.add(end);
						} else {
							// UNCHECKED
						}
					}
				} else {
					// contains one example (NO "such as")
					String example = source.substring(suchAs_ix + 9);
					String generalizer = source.substring(0, suchAs_ix);
					System.out.println(source + "\t" + example + "\t" + generalizer);
					new_concepts.add(generalizer);
					new_concepts.add(example);
				}
				for (String new_concept : new_concepts) {
					StringEdge new_edge = new StringEdge(new_concept, edge.getTarget(), edge.getLabel());
					new_edges.add(new_edge);

				}
				toRemove.add(edge);
			}
		}
		kb.addEdges(new_edges);
		kb.removeEdges(toRemove);
	}

	public static void correctText(StringGraph kb) {
		Set<String> concepts = kb.getVertexSet();
		for (String concept : concepts) {
			if (concept.startsWith("to "))
				continue;
			String before = concept;
			concept = concept.strip();
			// tratar conceitos começados por "various "
			int i = concept.indexOf("/");
			if (i != -1) {
				// get the first part
				System.out.println(concept);
			}
			kb.renameVertex(concept, before);
		}
	}

	public static void doStudy(StringGraph kb) throws IOException, URISyntaxException, InterruptedException {
//		ArrayList<String> initialConcepts = VariousUtils.readFileRows("newconcepts.txt");
//		HashSet<String> classes = new HashSet<>(GraphAlgorithms.getEdgesTargets(kb.edgeSet("isa")));
//		for (String concept : classes) {
//			
//			// get concepts that are super classes
//			Set<StringEdge> concept_isa = kb.outgoingEdgesOf(concept, "isa");
//			int degree = kb.incomingEdgesOf(concept, "isa").size();
//			if (concept_isa.isEmpty()) {
//				initialConcepts.add(concept);
//				System.out.printf("%s\t%d\n", concept, degree);
//			}
//			// System.lineSeparator();
//		}

		// -------------
//		// update constituency info using a llm
//		Set<String> concepts = kb.getVertexSet();
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
//		for (StringEdge edge : kb.edgeSet()) {
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
//		kb.removeEdges(toRemove);
//		kb.addEdges(toAdd);

//		System.exit(0);

//		OpenAiLLM_Caller.debugCaches();
//		System.exit(0);
		System.out.println(kb.edgesOf("c"));

		System.exit(0);

		// correct wrong NP/VP relations (source/target incorrect phrase)
		ArrayList<StringEdge> toRemove = new ArrayList<>(1 << 24);
		ArrayList<StringEdge> toAdd = new ArrayList<>(1 << 24);
		ReentrantLock rwlock = new ReentrantLock();
		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>();
		pc.parallelForEach(kb.edgeSet(), edge -> {
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
		kb.removeEdges(toRemove);
		kb.addEdges(toAdd);
		OpenAiLLM_Caller.saveCaches();
		System.out.printf("corrected %d edges\n", toRemove.size());

		// correct relations of the form:
		// bacteria,capableof,bacteria can synthesize vitamin
		// bob marley,desires,bob marley desires peace
//		ArrayList<StringEdge> toRemove = new ArrayList<>(1 << 24);
//		ArrayList<StringEdge> toAdd = new ArrayList<>(1 << 24);
//		ReentrantLock rwlock = new ReentrantLock();
//		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>();
//		pc.parallelForEach(kb.edgeSet(), edge -> {
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
//		kb.removeEdges(toRemove);
//		kb.addEdges(toAdd);
//		OpenAiLLM_Caller.saveCaches();
//		System.out.printf("corrected %d edges\n", toRemove.size());

		System.exit(0);
	}

	private static ArrayList<String> getAndOrComponents(String concept) {
		ArrayList<String> concepts = new ArrayList<String>();
		int or = concept.indexOf(" or ");
		int and = concept.indexOf(" and ");
		if (or != -1) {
			// not adding first because c0 might contain multiple concepts
			String c0 = concept.substring(0, or);
			String c1 = concept.substring(or + 4);
			concepts.add(c0);
			concepts.add(c1);
		} else if (and != -1) {
			// OK
			// not adding first because c0 might contain multiple concepts
			String c0 = concept.substring(0, and);
			String c1 = concept.substring(and + 5);
			concepts.add(c0);
			concepts.add(c1);
		} else {
			concepts.add(concept);
		}
		return concepts;
	}

	public static void getConceptPrefixHistogram(StringGraph kb) {
		ObjectCounter<String> prefix_counter = new ObjectCounter<String>();
		for (String concept : kb.getVertexSet()) {
			if (concept.startsWith(" ") || concept.startsWith("\t"))
				System.err.println(concept + " starts with a white space!");
			int is = concept.indexOf(" ");
			if (is >= 0) {
				String first_word = concept.substring(0, is);
				prefix_counter.addObject(first_word);
			}
		}
		prefix_counter.toSystemOut();
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		// read input space
		StringGraph kb = new StringGraph();
		String kb_filename = "new facts v3.csv";
		GraphReadWrite.readCSV(kb_filename, kb);

		// use this to clean the KB
//		OpenAiLLM_Caller.correctPluralConcepts(kb);
//		correctConceptsWithPronouns(kb);
//		correctCreatedByConcepts(kb);
//		correctConceptsWithIS(kb);
//		removeTextAfterParenthesis(kb);
//		correctText(kb);
//		correctSuchAsConcepts(kb);
//		correctedQuotedTextWithWordBy(kb);

//		OpenAiLLM_Caller.correctLifeFormConcept(kb);
//		OpenAiLLM_Caller.correctPluralConcepts(kb);

//		populateKB_withFileExamples(kb, "data/mais conceitos.txt");
//		populateKB_fromConceptList(kb, "data/newconcepts.txt");

		// fazer histograma da primeira palavra de cada conceito
//		getConceptPrefixHistogram(kb);
//		System.exit(0);

//		String conceptx = "preservative";
//		OpenAiLLM_Caller.getConceptHierarchy(conceptx);
//		System.out.println(OpenAiLLM_Caller.getIsaClass(conceptx));
//		populateKB_hierarchy(kb);
		GraphReadWrite.writeCSV(kb_filename, kb);
		System.exit(0);

		// testar hierarquia gerada pela LLM
		ArrayList<String> concepts = new ArrayList<String>(kb.getVertexSet());
		Collections.shuffle(concepts);
		for (int i = 0; i < concepts.size(); i++) {
			String concept = concepts.get(i);
			if (concept.startsWith("to "))
				continue;
			int num_words = VariousUtils.countWords(concept);
			if (num_words > 3)
				continue;
			OpenAiLLM_Caller.getConceptHierarchy(concept);
			Thread.sleep(2000);
		}
		System.exit(0);

//		System.exit(0);

		for (String concept : kb.getVertexSet()) {
			if (concept.startsWith("to "))
				continue;
			int word_count = VariousUtils.countWords(concept);
			if (word_count > 10) {
				ObjectCounter<String> relationCount = new ObjectCounter<String>();
				Set<StringEdge> edgesOf = kb.edgesOf(concept);
				for (StringEdge edge : edgesOf) {
					relationCount.addObject(edge.getLabel());
				}
				System.out.println(word_count + "\t" + concept + "\t" + edgesOf.size() + "\t" + relationCount);
			}

		}

		System.exit(0);

		for (String concept : kb.getVertexSet()) {
			if (concept.startsWith("to "))
				continue;
			int word_count = VariousUtils.countWords(concept);
			if (word_count > 3)
				continue;
			if (concept.contains(" or ") || concept.contains(" and ")) {
				Set<StringEdge> edgesOf = kb.edgesOf(concept);
				int num_edges = edgesOf.size();
				System.out.printf("%s\t%d\t%s\n", concept, num_edges, edgesOf);
			}
		}

		// GraphReadWrite.writeCSV("new facts v3.csv", kb);

		// System.exit(0);

//		for(StringEdge edge:kb.edgeSet()) {
//			String target = edge.getTarget();
//			String source = edge.getSource();
//			String label = edge.getLabel();
//		}

		// ------------
		// ------------
		// first KB building phase
//		OpenAiLLM_Caller.populateKB_withClassExamplesUsingPrompts(kb, "data/classes_and_prompts.txt");

//		// process invalid concepts in the KB
//		ArrayList<String> concepts = new ArrayList<>(kb.getVertexSet());
//		ConcurrentHashMap<String, String> conceptMap = new ConcurrentHashMap<>();
//		concepts.parallelStream().forEach(concept -> {
//			if (concept.indexOf('(') != -1 || concept.indexOf(')') != -1) {
//				Set<StringEdge> edgesOf = kb.edgesOf(concept);
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
		// OpenAiLLM_Caller.populateKB_expandFromExistingConcepts(kb);

		// ------------
		// ------------
		// individual KB treatments

		// ArrayList<String> toDelete = VariousUtils.readFileRows("concepts to delete.txt");
		// kb.removeVertices(toDelete);
		// kb.getVertexSet().parallelStream().forEach(vertex -> {
		// if (vertex.length() < 4) {
		// Set<StringEdge> edgesOf = kb.edgesOf(vertex);
		// System.out.printf("%s\t%d\t%s\n", vertex, edgesOf.size(), edgesOf);
		// }
		// });
		// doStudy(kb);

//		GraphReadWrite.writeCSV("new facts v3.csv", kb);

//		System.exit(0);
	}

	public static void correctedQuotedTextWithWordBy(StringGraph kb) {
		for (String concept : kb.getVertexSet()) {
			if (concept.startsWith("to "))
				continue;
			// this pattern only works for concepts containing a quote and the word " by "
			if (concept.contains("\"")) {
				int quote_occurrences = VariousUtils.countCharOccurences(concept, '\"');
				int index_of_by = concept.indexOf(" by ");
				boolean contains_by = index_of_by != -1;
				System.out.println(quote_occurrences + "\t" + concept + "\t" + contains_by);
				if (contains_by) {
					String creation = concept.substring(0, index_of_by);
					String creator = concept.substring(index_of_by + 4);
					kb.renameVertex(concept, creation);
					kb.addEdge(new StringEdge(creation, creator, "createdby"));
				}
			}
		}
	}

	public static void populateKB_hierarchy(StringGraph kb) throws InterruptedException, IOException {
		SynchronizedSeriarizableHashMap<String, Boolean> exploredConcepts = new SynchronizedSeriarizableHashMap<>("exploredConcepts.dat", 10);
		ArrayList<String> concepts = new ArrayList<String>(kb.getVertexSet());
		ParallelConsumer<String> pc = new ParallelConsumer<>(1);
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		System.out.printf("concept\tnum_edges\tnum_isa_source\tnum_isa_target\tremaining\n");
		pc.parallelForEach(concepts, concept -> {
			// ignore concepts that are verb phrase
			if (!exploredConcepts.containsKey(concept) && // that have been explored before
					!concept.startsWith("to ") && // that start with to
					!concept.contains(" or ") && // contain OR / AND
					!concept.contains(" and ") && //
					!concept.contains(" in ") && //
					true) {
				int num_words = VariousUtils.countWords(concept);
				if (num_words <= 3) {
					boolean is_entity = OpenAiLLM_Caller.checkIfConceptIsEntity(concept);
					if (is_entity) {
						boolean isSuperClass = OpenAiLLM_Caller.checkIfConceptIsSuperClass(concept);
						// System.out.printf("%s\t%s\n", concept, isSuperClass);
						ArrayList<StringEdge> localEdges = new ArrayList<>();
						// isa_out = concept,isa,X (concept is subclass of X)
						Set<StringEdge> isa_source;
						// isa_in = X,isa,concept (concept is superclass of X)
						Set<StringEdge> isa_target;
						Set<StringEdge> edgesOf;
						lock.readLock().lock();
						{
							isa_source = kb.outgoingEdgesOf(concept, "isa");
							isa_target = kb.incomingEdgesOf(concept, "isa");
							edgesOf = kb.edgesOf(concept);
						}
						lock.readLock().unlock();
						int num_isa_source = isa_source.size();
						int num_isa_target = isa_target.size();
						int num_edges = edgesOf.size();

						// all relations that are not ISA
						int remaining = num_edges - num_isa_target - num_isa_source;

						System.out.printf("%s\t%d\t%d\t%d\t%d\n", concept, num_edges, num_isa_source, num_isa_target, remaining);

						// adicionar arestas se dif for significativo

						// concept has a lot of information
						if (remaining >= 50) {
							// concept ISA almost OR nothing
							if (num_isa_source < 3000) {
								localEdges.addAll(OpenAiLLM_Caller.getConceptHierarchy(concept));
								System.lineSeparator();
							} else {
								// concept ISA many things
								// do not need more
								System.lineSeparator();
							}
							if (num_isa_target < 3) {
								// get examples of things that ISA concept
								// not for now
								System.lineSeparator();
							} else {
								// many things ISA concept
								// do not need more
								System.lineSeparator();
							}
							// dont ask for things that ISA,concept yet
							// few or no things ISA concept
//					if (isa_in.size() < 3) {
//						System.lineSeparator();
//					}
						} else // not much information about the concept, likely to be useless
						{
							// TODO ver se o conceito ISA qq coisa e usar isso como contexto

							// potentially add everything (common relations+hierarchy)
							ArrayList<StringEdge> hierarchy = OpenAiLLM_Caller.getConceptHierarchy(concept);
							StringEdge lastElement = hierarchy.get(hierarchy.size() - 1);
							ArrayList<StringEdge> relations_contextualized = OpenAiLLM_Caller.getAllRelationsContextualized_Serial(lastElement);
							if (isSuperClass) {
								ArrayList<StringEdge> examplesOf = OpenAiLLM_Caller.getExamplesOfClass(concept);
								localEdges.addAll(examplesOf);
							}
							localEdges.addAll(hierarchy);
							localEdges.addAll(relations_contextualized);

							System.lineSeparator();
						}
						// menos de 20 arestas e e origem de um ISA
						// obter contexto (isa) e popular com relacoes

						if (!localEdges.isEmpty()) {
							lock.writeLock().lock();
							{
								kb.addEdges(localEdges);
							}
							lock.writeLock().unlock();
						}
					}
				}
				exploredConcepts.put(concept, true);
			}
		});
		pc.shutdown();
		exploredConcepts.save();
	}

	public static void populateKB_fromConceptList(StringGraph kb, String filename) throws InterruptedException, IOException {
		SynchronizedSeriarizableHashMap<String, Boolean> exploredConcepts = new SynchronizedSeriarizableHashMap<>("exploredConcepts.dat", 10);
		ArrayList<String> conceptList = new ArrayList<String>();
		conceptList = VariousUtils.readFileRows(filename);
		ParallelConsumer<String> pc = new ParallelConsumer<>(8);
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		pc.parallelForEach(conceptList, concept -> {
			if (!exploredConcepts.containsKey(concept) && //
					!concept.startsWith("to ") && // that start with to
					!concept.contains(" or ") && // contain OR / AND
					!concept.contains(" and ") && //
					!concept.contains(" in ") && //
					true) {
				// ignore concepts that are verb phrase
				boolean is_entity = OpenAiLLM_Caller.checkIfConceptIsEntity(concept);
				if (is_entity) {
					ArrayList<StringEdge> threadLocalEdges = new ArrayList<StringEdge>();
					try {
						boolean isSuperClass = OpenAiLLM_Caller.checkIfConceptIsSuperClass(concept);
						if (isSuperClass) {
							ArrayList<StringEdge> examplesOf = OpenAiLLM_Caller.getExamplesOfClass(concept);
							threadLocalEdges.addAll(examplesOf);
							threadLocalEdges.add(new StringEdge(concept, "superclass", "isa"));
						}
						ArrayList<StringEdge> hierarchy = OpenAiLLM_Caller.getConceptHierarchy(concept);
						StringEdge lastElement = hierarchy.get(hierarchy.size() - 1);
						ArrayList<StringEdge> relations_contextualized = OpenAiLLM_Caller.getAllRelationsContextualized_Serial(lastElement);
						threadLocalEdges.addAll(hierarchy);
						threadLocalEdges.addAll(relations_contextualized);
					} catch (Exception e) {
						e.printStackTrace();
					}
					// push new edges into the KB
					lock.writeLock().lock();
					try {
						kb.addEdges(threadLocalEdges);
					} catch (Exception e) {
					} finally {
						lock.writeLock().unlock();
					}
				}
				exploredConcepts.put(concept, true);
			}
		});
		pc.shutdown();
	}

	public static void populateKB_FromConceptList_old(StringGraph kb, String filename) throws InterruptedException, IOException {
		ArrayList<String> initialConcepts = new ArrayList<String>();
		initialConcepts = VariousUtils.readFileRows(filename);

		final int blockSize = 32;
		final boolean exploreNewConcepts = false;

		ArrayDeque<String> openSet = new ArrayDeque<String>();
		HashSet<String> closedSet = new HashSet<String>();
		openSet.addAll(initialConcepts);

		File stopFile = new File("stop");
		ReentrantLock lock = new ReentrantLock();
		ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
		int counter = 0;

		while (!openSet.isEmpty()) {

			if (stopFile.exists())
				break;

			List<String> openSetBatch = VariousUtils.removeBlockFromDeque(openSet, blockSize);

			ArrayList<StringEdge> facts = new ArrayList<StringEdge>();

			ParallelConsumer<String> pc = new ParallelConsumer<>(NUMBER_OF_THREADS);
			pc.parallelForEach(openSetBatch, concept -> {
				concept = OpenAiLLM_Caller.cleanLine(concept);
				boolean conceptClosed = true;
				{
					rwLock.readLock().lock();
					conceptClosed = closedSet.contains(concept);
					rwLock.readLock().unlock();
				}
				// do not expand large concepts, or with many words, word already explored
				// TODO add option to be a target of an ISA edge and having a minimum amount of edges
				Set<StringEdge> existing_isa = kb.outgoingEdgesOf(concept, "isa");
				boolean minimum_ok = kb.edgesOf(concept).size() > 10;
				boolean flag = !existing_isa.isEmpty() && minimum_ok;

				if (flag && //
						concept.length() <= 32 && (VariousUtils.countCharOccurences(concept, ' ') + 1) <= 2 && !conceptClosed) {

					try {
						ArrayList<StringEdge> localEdges = new ArrayList<StringEdge>();
						localEdges.addAll(OpenAiLLM_Caller.getAllRelationsForConcept_Serial(concept));

//						// assuming the concept represents a class, otherwise this call might not make sense
//						ArrayList<StringEdge> examplesOfClass = OpenAiLLM_Caller.getExamplesOfClass(concept, kb);
//						// propagate existing relations in classType to the examples
//						ArrayList<String> newClasses = GraphAlgorithms.getEdgesSources(examplesOfClass);
//						ArrayList<StringEdge> inheritedRelations = GraphAlgorithms.propagateInheritance(newClasses, localEdges, concept);
//						localEdges.addAll(examplesOfClass);
//						localEdges.addAll(inheritedRelations);

						// get data for each example of each class
						ArrayList<StringEdge> isaEdges = OpenAiLLM_Caller.getExamplesOfClass(concept);
						localEdges.addAll(isaEdges);
						for (StringEdge isa_edge : isaEdges) {
							ArrayList<StringEdge> rel = OpenAiLLM_Caller.getAllRelationsContextualized_Serial(isa_edge);
							localEdges.addAll(rel);
						}

						{
							lock.lock();
							facts.addAll(localEdges);
							lock.unlock();
						}

						System.out.println(concept);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					// System.err.printf("1 skipping concept %s\n", concept);
				}
				{
					rwLock.writeLock().lock();
					closedSet.add(concept);
					rwLock.writeLock().unlock();
				}

			});
			pc.shutdown();

			closedSet.addAll(openSetBatch);
			int previousSize = openSetBatch.size();

			GraphReadWrite.writeCSV("newfacts" + counter + ".csv", facts);
			System.err.println("iteration " + counter + " done doing LLM calls, processing CoreNLP...");

			// collect into openConcepts the concepts from facts that are NP and not in the
			// closed set
			if (exploreNewConcepts) {
				HashSet<String> conceptsToAdd = new HashSet<String>();
				for (StringEdge fact : facts) {
					conceptsToAdd.add(fact.getSource());
					conceptsToAdd.add(fact.getTarget());
				}
				conceptsToAdd.parallelStream().forEach(concept -> {
					// do not explore lengthy concepts or with many words
					if (concept.length() <= 32 && (VariousUtils.countCharOccurences(concept, ' ') + 1) <= 3) {
						if (!closedSet.contains(concept)) {
							// only explore concepts that are NP
							try {
								String phraseType = PythonNLP_RestServiceInterface.getConstituencyLocalHostSpacy(concept);
								// String phraseType =
								// GrammarUtilsCoreNLP.getClassificationFromCoreNLP_raw(concept);
								if (phraseType.equals("NP")) {
									lock.lock();
									{
										openSet.add(concept);
									}
									lock.unlock();
								}
							} catch (Exception e) {
								if (lock.isHeldByCurrentThread())
									lock.unlock();
								e.printStackTrace();
							}
						}
					} else {
						// System.err.printf("2 skipping concept %s\n", concept);
					}
				});
			}

			System.err.printf("expanded %d concepts, %d new facts discovered, %d new concepts, %d total explored concepts\n", previousSize, facts.size(), openSet.size(),
					closedSet.size());
			VariousUtils.writeFileRows("newconcepts" + counter + ".txt", openSet);

			counter++;
		}
	}

	/**
	 * Gets data about a given class and examples of that class. The content of the file is prompt classname for each class example.
	 * 
	 */
	public static void populateKB_withClassExamplesUsingPrompts(StringGraph kb, String filename) throws IOException, InterruptedException {
		HashSet<String> closedConcepts = new HashSet<String>();

		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		ArrayList<String> fileRows = VariousUtils.readFileRows(filename);
		Iterator<String> it = fileRows.iterator();
		ArrayList<StringEdge> allIsaEdges = new ArrayList<StringEdge>();
		// first get data for each example of each class
		while (it.hasNext()) {
			String prompt = it.next().strip().toLowerCase();
			String classname = it.next().strip().toLowerCase();

			// get data for each class
			ArrayList<StringEdge> baseClassFacts = OpenAiLLM_Caller.getAllRelationsForConcept_Concurrent(classname);
			facts.addAll(baseClassFacts);

			// get data for each example of each class
			ArrayList<StringEdge> isaEdges = OpenAiLLM_Caller.getExamplesOf(prompt, classname);
			allIsaEdges.addAll(allIsaEdges);
			ArrayList<StringEdge> fullRelations = OpenAiLLM_Caller.getAllRelationsContextualized_Concurrent(isaEdges);
			facts.addAll(fullRelations);

			closedConcepts.add(classname);
			for (StringEdge edge : isaEdges) {
				closedConcepts.add(edge.getSource());
			}
		}
		VariousUtils.writeFileRows("data/closed concepts.txt", closedConcepts);

		// add back the new facts to the kb
		kb.addEdges(facts);
	}

	/**
	 * Gets data about a given class and examples of that class. Each row of the given file must be of the form name \t class
	 * 
	 */
	public static void populateKB_withFileExamples(StringGraph kb, String filename) throws IOException, InterruptedException {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();

		// row file format is entity TAB class
		HashMap<String, String> fileRows = VariousUtils.readTwoColumnFile(filename, "\t");

		// convert the hashmap to an arraylist supported by the ParallelConsumer
		ArrayList<Entry<String, String>> row_list = new ArrayList<Entry<String, String>>();
		Iterator<Entry<String, String>> it = fileRows.entrySet().iterator();

		// first get data for each example of each class
		while (it.hasNext()) {
			Entry<String, String> next = it.next();
			row_list.add(next);
		}

		ReentrantLock lock = new ReentrantLock();

		ParallelConsumer<Entry<String, String>> pc = new ParallelConsumer<>(NUMBER_OF_THREADS);
		pc.parallelForEach(row_list, row -> {
			String name = row.getKey().strip().toLowerCase();
			String clazz = row.getValue().strip().toLowerCase();

			StringEdge context = new StringEdge(name, clazz, "isa");
			ArrayList<StringEdge> fullRelations = OpenAiLLM_Caller.getAllRelationsContextualized_Serial(context);
			{
				lock.lock();
				facts.addAll(fullRelations);
				lock.unlock();
			}
		});
		pc.shutdown();

		// add back the new facts to the kb
		kb.addEdges(facts);
	}

	public static void removeTextAfterParenthesis(StringGraph kb) {
		Set<String> concepts = kb.getVertexSet();
		for (String concept : concepts) {
			// if (concept.startsWith("to "))
			// continue;
			int p0 = concept.indexOf("(");
			int p1 = concept.indexOf(")");
			if (p0 == -1 || p1 == -1)
				continue;
			String before = concept.substring(0, p0 - 1);
			// String between = concept.substring(p0 + 1, p1);
			// String after = concept.substring(p1 + 1);
			// System.out.println(before + "\t" + between + "\t" + after);
			System.out.printf("%s\t->\t%s\n", concept, before);
			kb.renameVertex(concept, before);
		}
	}

}
