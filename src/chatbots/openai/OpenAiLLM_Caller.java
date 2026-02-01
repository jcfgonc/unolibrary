package chatbots.openai;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import indefinitearticle.IndefiniteArticle;
import io.github.sashirestela.openai.OpenAI.ChatCompletions;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import stream.ParallelConsumer;
import structures.SynchronizedSeriarizableHashMap;
import utils.InvocationsPerMinuteTracker;
import utils.OSTools;
import utils.VariousUtils;

public class OpenAiLLM_Caller {

	private static String api_key;
	private static SynchronizedSeriarizableHashMap<String, String> cachedConceptIsLifeform = new SynchronizedSeriarizableHashMap<>("cachedConceptIsLifeform.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedConceptIsPlural = new SynchronizedSeriarizableHashMap<>("cachedConceptIsPlural.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedConceptIsEntity = new SynchronizedSeriarizableHashMap<>("cachedConceptIsEntity.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedConceptIsSuperClass = new SynchronizedSeriarizableHashMap<>("cachedConceptIsSuperClass.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedNP_to_VP = new SynchronizedSeriarizableHashMap<>("cachedNP_to_VP.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedPlural2Singular = new SynchronizedSeriarizableHashMap<>("cachedPlural2Singular.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedRawPhrases = new SynchronizedSeriarizableHashMap<>("cachedRawPhrases.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedVP_to_NP = new SynchronizedSeriarizableHashMap<>("cachedVP_to_NP.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedConceptHasExamples = new SynchronizedSeriarizableHashMap<>("cachedConceptHasExamples.dat", 30);
	private static SynchronizedSeriarizableHashMap<String, String> cachedRelatedConcepts = new SynchronizedSeriarizableHashMap<>("cachedRelatedConcepts.dat", 30);

	private static ChatCompletions chatCompletions;
	private static final double FREQUENCY_PENALTY = 0.25;
	public static final String llm_model = "gpt-4o";
	private static final int MAX_COMPLETION_TOKENS = 128;
	private static Set<String> nounPhrases_fromFile;
	private static final String NP_FILENAME = "D:\\My Source Code\\Java - PhD\\UnoLibrary\\data\\noun_phrases.txt";

	private static final int NUMBER_OF_THREADS = 16;

	private static SimpleOpenAI openAI;

	private static final String PROMPT_INTRO = """
			You are a knowledge base that answers to a single question made by an expert system.
			Your answers will be interpreted by an english expert system and stored in a knowledge graph.
			Your knowledge base is in American English.
			You have a comprehensive ontology and knowledge that spans the basic concepts and rules about how the world works.
			You answer to the question with multiple factual possibilities in non-formatted text. Do not fancy or pretty format your answer.
			Do not explain your reason or your answer. Do not enumerate your answer.
			Reply each possible answer in its own line, one possibility per line.

			""";

	private static final double TEMPERATURE = 0.000;

	static {
		try {
			api_key = VariousUtils.readFile("data/openai_api_key.txt");

			nounPhrases_fromFile = new HashSet<String>();
			if (VariousUtils.checkIfFileExists(NP_FILENAME)) {
				ArrayList<String> rows = VariousUtils.readFileRows(NP_FILENAME);
				nounPhrases_fromFile.addAll(rows);
			}
			// set read only
			nounPhrases_fromFile = Collections.unmodifiableSet(nounPhrases_fromFile);

		} catch (IOException e) {
			// no api key, forget it
			e.printStackTrace();
			System.exit(-1);
		}
		openAI = SimpleOpenAI.builder().apiKey(api_key).build();
		chatCompletions = openAI.chatCompletions();
	}

	public static boolean checkIfConceptIsEntity(String concept) {
		String prompt = """
				Answer yes or no if as a concept stored in a knowledge base,
				the concept "%s" represents either an entity, a class, or a group of entities.""";
		String text = String.format(prompt.strip(), concept);
		String reply = cachedConceptIsEntity.get(concept);
		if (reply == null) {
			reply = doRequest(text).toLowerCase().strip();
			cachedConceptIsEntity.put(concept, reply);
		}

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfConceptHasExamples(String concept) {
		String prompt = PROMPT_INTRO + """
				Answer yes or no if as a concept stored in a knowledge base,
				the concept "%s" ontologically has a subsumption relation to more specific instances of "%s".""";
		String text = String.format(prompt.strip(), concept, concept);
		String reply = cachedConceptHasExamples.get(concept);
		if (reply == null) {
			reply = doRequest(text).toLowerCase().strip();
			cachedConceptHasExamples.put(concept, reply);
		}

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfConceptIsLifeForm(String entity) {
		String reply = cachedConceptIsLifeform.get(entity);
		if (reply == null) {
			String prompt = "Answer yes or no to the following question: is the text \"%s\" referring to a life form?";
			String text = String.format(prompt.strip(), entity);
			reply = doRequest(text).toLowerCase().strip();
			cachedConceptIsLifeform.put(entity, reply);
		}
		boolean yes = reply.startsWith("yes");
		boolean no = reply.contains("no");
		if (yes && no) {
			System.err.println("alive and dead!:::" + reply);
		}
		if (no)
			return false;
		if (yes)
			return true;
		System.err.println("WTF:not alive neither dead!:::" + reply);
		return false;
	}

	public static boolean checkIfConceptsAreRelated(String entity0, String entity1) {
		String entities = entity0 + "|" + entity1;
		String reply = cachedRelatedConcepts.get(entities);
		if (reply == null) {
			String prompt = """
					Do not answer metaphorical, analogical or any other figure of speech that shows linguistic creativity.
					Answer only straightforward facts with literal meaning.
					Is there a relationship between \"%s\" and \"%s\" ?
					""";
			String text = String.format(prompt.strip(), entity0, entity1);
			reply = doRequest(text).toLowerCase().strip();
			cachedRelatedConcepts.put(entities, reply);
		}
		boolean yes = reply.startsWith("yes");
		boolean no = reply.contains("no");
		if (yes && no) {
			System.err.println("alive and dead!:::" + reply);
		}
		if (no)
			return false;
		if (yes)
			return true;
		System.err.println("WTF:not alive neither dead!:::" + reply);
		return false;
	}

	public static boolean checkIfConceptIsSingular(String entity) {
		String reply = cachedConceptIsPlural.get(entity);
		if (reply == null) {
			String prompt = "Only answer singular or plural, what is the grammatical number of \"%s\"?";
			String text = String.format(prompt.strip(), entity);
			reply = doRequest(text).toLowerCase().strip();
			cachedConceptIsPlural.put(entity, reply);
		}
		reply = reply.replace("\r", "");
		reply = reply.replace("\n", " ");
		boolean singular = reply.contains("singular");
		boolean plural = reply.contains("plural");
		if (singular && plural) {
			System.err.println("WTF:plural and singular! " + entity + " reply=" + reply);
			return true;
		}
		if (singular)
			return true;
		if (plural)
			return false;
		// likely both
		System.err.println("WTF: neither plural nor singular! " + entity + " reply=" + reply);
		return true;
	}

	public static boolean checkIfEntityCreates(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s create anything?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasCapabilities(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s have abilities?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasCreator(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Does %s have a creator, parents or an origin?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasDesires(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s have desires?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasMotivesOrGoals(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s have goals to achieve?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasNotableIdeas(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Is %s known to have notable ideas?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasParts(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Does %s have any parts or components?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasRequirements(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s require anything or %s have pre-conditions?""";
		String text = String.format(prompt.strip(), entity, entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityIsMadeOfSomething(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Is in the present or was in the past %s made of something?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfConceptIsSuperClass(String concept) {
		String prompt = """
				Answer yes or no. Ontologically, is the concept "%s" a superclass?""";
		String text = String.format(prompt.strip(), concept);
		String reply = cachedConceptIsSuperClass.get(concept);
		if (reply == null) {
			reply = doRequest(text).toLowerCase().strip();
			cachedConceptIsSuperClass.put(concept, reply);
		}
//		System.out.println("checkIfEntityIsSuperClass() " + concept + ": " + reply);

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static void checkISA(StringGraph graph) throws IOException, URISyntaxException {
		ArrayList<StringEdge> trueFacts = new ArrayList<StringEdge>();
		ArrayList<StringEdge> falseFacts = new ArrayList<StringEdge>();
		final int batchSize = 10;
		int counter = 0;
		final int queryLimit = 150;
		ArrayDeque<StringEdge> edges = new ArrayDeque<StringEdge>(graph.edgeSet("isa"));

		while (!edges.isEmpty()) {
			if (counter >= queryLimit)
				break;
			ArrayList<StringEdge> edges_sublist = VariousUtils.removeMultipleElementsFromDeque(edges, batchSize);
			try {
				BooleanArrayList results = verifyISA_facts(edges_sublist);
				for (int i = 0; i < edges_sublist.size(); i++) {
					StringEdge edge = edges_sublist.get(i);
					boolean valid = results.getBoolean(i);
					// System.err.printf("%s\t%s\n", edge, valid);
					if (valid) {
						trueFacts.add(edge);
					} else {
						falseFacts.add(edge);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.printf(":: %d\n", counter);
			counter++;
		}
		GraphReadWrite.writeCSV("truefacts.csv", trueFacts);
		GraphReadWrite.writeCSV("falseFacts.csv", falseFacts);
		// both have been verified
		graph.removeEdges(trueFacts);
		graph.removeEdges(falseFacts);
	}

	public static void checkISA_concurrent(StringGraph graph) throws IOException, URISyntaxException {
		final int batchSize = 10;
		final int queryLimit = 6000;
		final int numThreads = OSTools.getNumberOfLogicalProcessors();

		ArrayList<StringEdge> trueFacts = new ArrayList<StringEdge>();
		ArrayList<StringEdge> falseFacts = new ArrayList<StringEdge>();
		AtomicInteger globalQueryCounter = new AtomicInteger(0);
		ReentrantLock dequeLock = new ReentrantLock();
		ReentrantLock exitLock = new ReentrantLock();
		ArrayDeque<StringEdge> edges = new ArrayDeque<StringEdge>(graph.edgeSet("isa"));

		if (edges.isEmpty()) {
			System.err.println("empty edge list, aborting");
			return;
		}

		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		File stopFile = new File("stop");
		System.out.println(VariousUtils.getCurrentWorkingDirectory());

		for (int tn = 0; tn < numThreads; tn++) {
			executorService.execute(new Runnable() {

				public void run() {
					// local storage for fact verification
					ArrayList<StringEdge> local_trueFacts = new ArrayList<StringEdge>();
					ArrayList<StringEdge> local_falseFacts = new ArrayList<StringEdge>();

					boolean flag = true;

					while (flag) {

						if (stopFile.exists())
							flag = false;

						// extract from global edges list to local/thread storage
						ArrayList<StringEdge> local_edges = new ArrayList<StringEdge>();
						dequeLock.lock();
						for (int i = 0; i < batchSize && !edges.isEmpty(); i++) {
							local_edges.add(edges.removeLast());
						}
						dequeLock.unlock();

						try {
							// verify facts
							BooleanArrayList results = verifyISA_facts(local_edges);
							if (results.size() != local_edges.size()) {
								System.err.printf("got %d results from %d facts!!!\n", results.size(), local_edges.size());
								continue;
							}
							// store results locally
							for (int i = 0; i < local_edges.size(); i++) {
								StringEdge edge = local_edges.get(i);
								boolean valid = results.getBoolean(i);
								// System.err.printf("%s\t%s\n", edge, valid);
								if (valid) {
									local_trueFacts.add(edge);
								} else {
									local_falseFacts.add(edge);
								}
							}
							int counter = globalQueryCounter.incrementAndGet();
							if (counter >= queryLimit) {
								flag = false;
							}
							System.out.printf(":: %d\n", counter);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					exitLock.lock();
					trueFacts.addAll(local_trueFacts);
					falseFacts.addAll(local_falseFacts);
					exitLock.unlock();
				}
			});
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		GraphReadWrite.writeCSV("truefacts.csv", trueFacts);
		GraphReadWrite.writeCSV("falseFacts.csv", falseFacts);
		// both have been verified
		graph.removeEdges(trueFacts);
		graph.removeEdges(falseFacts);
	}

	public static String cleanLine(String line) {
//		String initial_line = line;
		line = line.strip();

		// commented because the (???) text is required for relation context
//		line = line.replaceAll("\\s*\\(.+\\).*$", ""); // remove text between parentheses and text that follows until the end of string

		line = line.replaceAll("\\(\\s+", "("); // space ( space
		line = line.replaceAll("\\s+\\)", ")");// space ) space

//		line = line.replaceAll("[^a-zA-Z \\-'0-9\\r\\n]+", " "); // remove all non text characters (excluding hyphen "-" )
		line = line.replaceFirst("^[0-9]+[\\-.:]+[\\s]*", ""); // remove enumeration ie 12. something OR 1 something
//		String test = line.replaceAll("^[\\d]+[ \\t]+[\\w ]+", ""); // number followed by text
//		if (test.isEmpty()) {
//			line = line.substring(line.indexOf(' ') + 1);
//		}
		line = line.replaceAll("[\\d\\w]+:", ""); // class: OR kingdom:
		line = line.replaceAll("[ \t]+", " "); // multiple whitespace -> space

		//
//		int size = line.length();
//		if (size >= 64) {
//			System.err.printf("cleanLine() large concept with size\t%d\ttext\t%s\n", size, line);
//			line = line.substring(0, 64);
//			int lastSpace_i = line.lastIndexOf(' ');
//			if (lastSpace_i != -1) {
//				line = line.substring(0, lastSpace_i);
//			}
//		}
		// simplify sentence
		String target = line;
//		try {
//			target = PythonNLP_RestServiceInterface.stripDeterminantsAndSingularize(line);
//		} catch (Exception e) {
//			e.printStackTrace();
//			target = "35408228dbbc03585527412fb9065853e85c2e7f2be2f38bab88ca20458423021dac4f52b5e73279411e74d7ae34b2ae3760d54fef3057c0bca49a34d761478c";
//			System.err.printf("cleanLine() remote NLP call failed, concept %s converted to %s\n", line, target);
//		}
		// GrammarUtilsCoreNLP.stripDeterminantsAndSingularize(line);
		// target = target.replace("- ", "");
		// System.out.printf("%s\t->\t%s\n", initial_line, target);
		return target;
	}

	/**
	 * this function MUST maintain the presence of newlines \n; "\r" is removed
	 * 
	 * @param reply
	 * @return
	 */
	public static String cleanReply(String reply) {
		reply = reply.strip();
		reply = reply.replaceAll("\\s*[,]+\\s*", ";"); // space*,space* -> ' ' no commas can go into here, because of the CSV
														// format
		reply = reply.replaceAll("[ \t]+", " ");// multiple whitespace -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replaceAll("[\t ]+", " "); // multiple white space to space
		reply = reply.replaceAll("[\s]+\n", "\n"); // space newline -> new line
		return reply;
	}

	public static String convertNP_to_VP(String phrase) {
		String reply = cachedNP_to_VP.get(phrase);
		if (reply == null) {
			String prompt = """
					Do not explain your reasoning. Answer only what is asked. Convert the following text to
					the semantically equivalent infinite verb form with the to particle:

					%s
					""";
			String text = String.format(prompt.strip(), phrase);

			reply = doRequest(text).toLowerCase().strip();
			cachedNP_to_VP.put(phrase, reply);
		}

		reply = reply.replaceAll("[\r\n]+", " "); // newlines
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space

		reply = correctVP_NP_reply(reply);

		if (reply.contains("phrase") || reply.contains("equivalent")) {
			System.out.printf("NP2VP\t%s\t%d\t%s\n", phrase, phrase.length(), reply);
			// System.lineSeparator();
		}

		// System.out.printf("convertNP_to_VP\t%s\n", reply);
		return reply;
	}

	public static String convertPlural2Singular(String plural) {
		String reply = cachedPlural2Singular.get(plural);
		if (reply == null) {
			String prompt = """
					Do not elaborate your answer. Do not explain your answer. Only answer with the converted text. Convert the following noun phrase to the singular form:
					%s
					""";
			String text = String.format(prompt.strip(), plural);
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replaceAll("[\r\n]+", " "); // newlines
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			cachedPlural2Singular.put(plural, reply);
		}
		return reply;
	}

	public static String convertVP_to_NP(String phrase) {
		String reply = cachedVP_to_NP.get(phrase);
		if (reply == null) {
			String prompt = """
					Do not explain your reasoning. Answer only what is asked. Convert the following text to
					the semantically equivalent noun phrase. Remove all the verbs, particles and articles:

					%s
					""";
			String text = String.format(prompt.strip(), phrase);

			reply = doRequest(text).toLowerCase().strip();
			cachedVP_to_NP.put(phrase, reply);
		}

		reply = reply.replaceAll("[\r\n]+", " "); // newlines
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space

		reply = correctVP_NP_reply(reply);

		if (reply.contains("phrase") || reply.contains("equivalent")) {
			System.out.printf("VP2NP\t%s\t%d\t%s\n", phrase, phrase.length(), reply);
			// System.lineSeparator();
		}

		// System.out.printf("convertVP_to_NP\t%s\n", reply);
		return reply;
	}

	/**
	 * if the given edge can be corrected, returns the corrected edge, otherwise returns null
	 * 
	 * @param edge
	 * @return
	 */
	public static StringEdge correctRelation(StringEdge edge) {
		/**
		 * relations to verify: atlocation capableof causes causesdesire createdby desires isa knownfor madeof notdesires partof requires usedto
		 */
		String source = edge.getSource().toLowerCase();
		String target = edge.getTarget().toLowerCase();
		String label = edge.getLabel().toLowerCase();

		String cons_source = getPhraseType(source);
		String cons_target = getPhraseType(target);

		boolean source_np = cons_source.equals("NP");
		boolean target_np = cons_target.equals("NP");
		boolean target_vp = cons_target.equals("VP") && target.startsWith("to ");

		boolean toCorrect = false;
		String newSource = source;
		String newTarget = target;

		switch (label) {
		case "atlocation": // DONE 1
			// NP atlocation NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "capableof": // DONE 2
			// NP capableof NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "causes": // DONE 1
			// NP causes NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "causesdesire": // DONE 1
			// NP causesdesire VP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_vp) {
				// correct target
				newTarget = convertNP_to_VP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "createdby": // DONE 1
			// NP createdby NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "isa": // DONE
			// NP isa NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "knownfor": // DONE
			// NP knownfor NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "madeof": // DONE
			// NP madeof NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "desires": // DONE 1
		case "notdesires": // DONE
			// NP desires <>
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "partof": // DONE
			// NP partof NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "requires": // DONE
			// NP requires NP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_np) {
				// correct target
				newTarget = convertVP_to_NP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		case "usedto": // DONE
			// NP usedto VP
			if (!source_np) {
				// correct source
				newSource = convertVP_to_NP(source);
				toCorrect = true;
			}
			if (!target_vp) {
				// correct target
				newTarget = convertNP_to_VP(target);
				toCorrect = true;
			}
			if (toCorrect) {
				StringEdge newEdge = new StringEdge(newSource, newTarget, label);
				return newEdge;
			}
			break;

		}
//		System.out.printf("%s\t%s,%s,%s\n", edge, cons_source, label, cons_target);
		return null;
	}

	/**
	 * corrects text replied by the llm in the functions convertVP_to_NP and convertNP_to_VP
	 * 
	 * @param text
	 * @return
	 */
	public static String correctVP_NP_reply(String reply) {
//		String org_reply = reply;
		if (reply.startsWith("-")) {
			reply = reply.substring(1).trim();
		} else if (reply.contains("→")) {
			reply = reply.substring(reply.indexOf('→') + 1).trim();
		}
//		if (!reply.equals(org_reply)) {
//			System.err.printf("WARNING\t%s\t%s\n", org_reply, reply);
//		}
		return reply;
	}

	public static void debugCaches() {
		OpenAiLLM_Caller.cachedNP_to_VP.debug();
		OpenAiLLM_Caller.cachedVP_to_NP.debug();
	}

	/**
	 * does the raw OpenAI API request call
	 * 
	 * @param prompt
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String doRequest(String prompt) {
		while (true) {
			try {
				InvocationsPerMinuteTracker.printInvocationsPerMinute("OpenAI call");
				ChatRequest chatRequest = ChatRequest.builder().model(llm_model).message(UserMessage.of(prompt)).temperature(TEMPERATURE).maxCompletionTokens(MAX_COMPLETION_TOKENS)
						.frequencyPenalty(FREQUENCY_PENALTY).presencePenalty(0.05).build();
				CompletableFuture<Chat> futureChat = chatCompletions.create(chatRequest);
				Chat chatResponse = futureChat.join();
				String firstContent = chatResponse.firstContent();
				return firstContent;
			} catch (CompletionException e) {
				System.err.println("doRequest: " + e.getMessage());
				// HTTP interaction failed: server returned a 429 response status.
				// 429 error means that query rate limit has been Exceeded
				if (e.getMessage().contains("429")) {
				}
			} catch (Exception e) {
				System.err.println("doRequest: " + e.getMessage());
				e.printStackTrace();
			}
			// do a pause and retry again
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receives a list of ISA facts and populates each fact source with relations from a LLM. (Parallelized)
	 * 
	 * @param isaEdges
	 * @return
	 * @throws InterruptedException
	 */
	public static ArrayList<StringEdge> getAllRelationsContextualized_Concurrent(Collection<StringEdge> isaEdges) throws InterruptedException {

		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();

//		// serial version
//		for (StringEdge isaEdge : isaEdges) {
//			facts.addAll(getAllRelationsContextualized(isaEdge));
//		}

		// parallel version
		ReentrantLock lock = new ReentrantLock();
		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>(NUMBER_OF_THREADS);
		try {
			pc.parallelForEach(isaEdges, isaEdge -> {

				ArrayList<StringEdge> allRelations = getAllRelationsContextualized_Serial(isaEdge);
				lock.lock();
				{
					facts.addAll(allRelations);
				}
				lock.unlock();
			});
			pc.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return facts;
	}

	public static ArrayList<StringEdge> getAllRelationsContextualized_Serial(StringEdge isaEdge) {
		System.out.println(isaEdge);
		// edge must be an ISA relation for this function call to be correct
		assert isaEdge.getLabel().equals("isa");

		// formats as concept (type)
		String concept = isaEdge.getSource();
		String type = isaEdge.getTarget();
		String conceptWithContext = String.format("%s (%s)", concept, type);
		ArrayList<StringEdge> localEdges = getAllRelationsForConcept_Serial(conceptWithContext);

		ArrayList<StringEdge> tempEdges = new ArrayList<StringEdge>();
		for (StringEdge t_edge : localEdges) {
			tempEdges.add(t_edge.replaceSourceOrTarget(conceptWithContext, concept));
		}

		return tempEdges;
	}

	public static ArrayList<StringEdge> getAllRelationsForConcept_Concurrent(String concept) throws InterruptedException {
		ArrayList<StringEdge> localEdges = new ArrayList<StringEdge>();
		ReentrantLock lock = new ReentrantLock();

		System.out.printf("getting all relations for %s\n", concept);

		ExecutorService executor = Executors.newFixedThreadPool(OSTools.getNumberOfLogicalProcessors());

		executor.submit(() -> {
			System.out.printf("getCapableOf %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getCapableOf(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getCauses %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getCauses(concept));
			lock.unlock();
		});
//		executor.submit(() -> {
//			System.out.printf("getCausesDesire %s\n", concept);
//			lock.lock();
//			localEdges.addAll(OpenAiLLM_Caller.getCausesDesire(concept));
//			lock.unlock();
//		});
		executor.submit(() -> {
			System.out.printf("getCreatedBy %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getCreatedBy(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getCreates %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getCreates(concept));
			lock.unlock();
		});

		if (OpenAiLLM_Caller.checkIfConceptIsLifeForm(concept)) {
			lock.lock();
			localEdges.add(new StringEdge(concept, "lifeform", "isa"));
			lock.unlock();
			executor.submit(() -> {
				System.out.printf("getDesires %s\n", concept);
				lock.lock();
				localEdges.addAll(OpenAiLLM_Caller.getDesires(concept));
				lock.unlock();
			});
			executor.submit(() -> {
				System.out.printf("getNotDesires %s\n", concept);
				lock.lock();
				localEdges.addAll(OpenAiLLM_Caller.getNotDesires(concept));
				lock.unlock();
			});
		}

		executor.submit(() -> {
			System.out.printf("getIsaClass %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getIsaClass(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getKnownFor %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getKnownFor(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getMadeOf %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getMadeOf(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getPartOf %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getPartOf(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getRequires %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getRequires(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getUsedTo %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getUsedTo(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getWhatIsPartOf %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getWhatIsPartOf(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getAtLocation %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getAtLocation(concept));
			lock.unlock();
		});
		executor.submit(() -> {
			System.out.printf("getLimitationsOf %s\n", concept);
			lock.lock();
			localEdges.addAll(OpenAiLLM_Caller.getLimitationsOf(concept));
			lock.unlock();
		});

		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

		return localEdges;
	}

	public static ArrayList<StringEdge> getAllRelationsForConcept_Serial(String concept) {
		System.out.printf("getting all relations for %s\n", concept);
		ArrayList<StringEdge> localEdges = new ArrayList<StringEdge>();
		System.out.printf("getCapableOf %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getCapableOf(concept));
		System.out.printf("getCauses %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getCauses(concept));
//		System.out.printf("getCausesDesire %s\n", concept);
//		localEdges.addAll(OpenAiLLM_Caller.getCausesDesire(concept));
		System.out.printf("getCreatedBy %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getCreatedBy(concept));
		System.out.printf("getCreates %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getCreates(concept));
		System.out.printf("getIsaClass %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getIsaClass(concept));
		System.out.printf("getKnownFor %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getKnownFor(concept));
		System.out.printf("getMadeOf %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getMadeOf(concept));

		if (OpenAiLLM_Caller.checkIfConceptIsLifeForm(concept)) {
			System.out.printf("getDesires %s\n", concept);
			localEdges.addAll(OpenAiLLM_Caller.getDesires(concept));
			System.out.printf("getNotDesires %s\n", concept);
			localEdges.addAll(OpenAiLLM_Caller.getNotDesires(concept));
			localEdges.add(new StringEdge(concept, "lifeform", "isa"));
		}

		System.out.printf("getPartOf %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getPartOf(concept));
		System.out.printf("getRequires %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getRequires(concept));
		System.out.printf("getUsedTo %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getUsedTo(concept));
		System.out.printf("getWhatIsPartOf %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getWhatIsPartOf(concept));
		System.out.printf("getAtLocation %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getAtLocation(concept));
		System.out.printf("getLimitationsOf %s\n", concept);
		localEdges.addAll(OpenAiLLM_Caller.getLimitationsOf(concept));
		return localEdges;
	}

	/**
	 * obtains the places where the entity is located at. Correct form is NP atlocation NP.
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getAtLocation(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about the locations or places that a given entity is usually at or located at.
				You list the most important locations.
				A location can be a common noun or a proper noun, such as a city, village, country, etc.
				Answer each location as a noun phrase.

				Where is %s located?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "atlocation");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the capabilities of the given entity. correct form is NP capableof NP.
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCapableOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasCapabilities(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about what an entity is capable of or what an entity is able to.
				You answer the most important and specific entity’s capabilities.
				You answer each capability as an infinite verb form that starts with the to particle.

				What is %s capable of?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		String request = doRequest(text);
		reply = cleanReply(request.toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			// likely text:
			// X be capable of Y
			if (target.startsWith(entity + " ")) {
				target = target.substring(entity.length() + 1);
			}
			String prefix = "be capable of ";
			if (target.startsWith(prefix)) {
				target = target.substring(prefix.length());
			}
			StringEdge edge = new StringEdge(entity, target, "capableof");
			facts.add(edge);
		}
		return facts;
	}

	/**
	 * obtains the effects that the given entity causes
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCauses(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about the impact that the given entity has on other entities, as well as its consequences or what effects the
				entity has on other entities.
				You list the most impacts or consequences of that entity.
				You only answer the impacts or consequences that you are certain to be caused by all entities of that type.
				An impact or consequence may be something that the entity causes either by existing or by
				interacting with something, such as occasions or events.
				Do not answer the purposes, do not list its objectives nor the goals of	that entity.
				The purpose of an entity is not the same as its consequences nor the same as its impacts.
				Answer each consequence of the entity as a noun phrase.

				What consequences or impacts does %s cause?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		String strip = doRequest(text).toLowerCase().strip();
		reply = cleanReply(strip);

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);

			int ix = target.indexOf("cause ");
			if (ix >= 0) {
				target = target.substring(ix + 6);
			}
			StringEdge edge = new StringEdge(entity, target, "causes");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the desires that the given entity causes
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCausesDesire(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about the desires that an entity elicits in other entities.
				A desire may be conscious impulses towards something that promises enjoyment or satisfaction in its attainment, longing or craving
				or a sudden spontaneous inclination or incitement to some usually unpremeditated action.
				You list the most important desires.
				You only list the desires that all entities of that type elicit on other entities.
				You list each elicited desire as an infinite verb form that starts with the to particle.
				List one elicited desire per line.

				What desires does %s elicit in another?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "causesdesire");
			facts.add(edge);
		}

		return facts;
	}

	public static ArrayList<StringEdge> getConceptHierarchy(String entity) {
		entity = entity.toLowerCase();
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = """
				In terms of Ontological Classification, summarize the conceptual hierarchy of "%s" as an unformatted list. Stop the list at %s and do not give examples of %s.
				""";
		String text = String.format(prompt.strip(), entity, entity, entity);

		String original_reply = doRequest(text);
		String processed_reply = cleanReply(original_reply.toLowerCase().strip());

		String[] lines = processed_reply.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String clean = cleanLine(lines[i]);
			// remove " --- "
			clean = clean.replaceAll("^[\\s]*[-]*[\\s]*", "").strip();
			clean = removeTextAfterParentheses(clean).strip();
			lines[i] = clean;
		}

		for (int i = 1; i < lines.length; i++) {
			String current = lines[i];
			String previous = lines[i - 1];
			StringEdge edge = new StringEdge(current, previous, "isa");
			facts.add(edge);
			// prevent hallucination bug
			if (current.equals(entity))
				break;
			// will add a duplicate edge at the last iteration but that should be a problem
//			edge = new StringEdge(entity, previous, "isa");
//			facts.add(edge);
		}
//		Collections.reverse(facts);
//		System.out.println(entity + "->" + facts);
//		System.out.println(entity + "\r\n->" + original_reply + "\n" + facts);
		return facts;
	}

	/**
	 * obtains the entities that created or create the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCreatedBy(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasCreator(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about who or what created a given entity.
				You answer each creator as a noun phrase or with its proper name, whichever is more specific.
				A creator may be a person, a collective, a company or any another type of entity.
				If the asked entity is a person or an animal, you name its parents or progenitors.
				Answer all possible creators.

				Who or what created %s?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "createdby");
			facts.add(edge);
		}
		return facts;
	}

	/**
	 * obtains entities created by the given entity (or entities that the given entity creates)
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCreates(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityCreates(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about what a given entity can create or originate.
				You answer each creation as a noun phrase or with its name.
				A creation may be a person, a collective, a single entity, a company or any another type of entity.
				If the asked entity is a person or an animal, you name its successors or children.
				Answer all possible creations.

				Who or what does %s create?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(target, entity, "createdby");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the desires of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getDesires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasDesires(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about an entity and what that entity desires, what that entity wishes, what that entity wants
				or what that entity hopes for.
				You answer the most important desires by that entity.
				You only answer the desires that you are certain to be desired by all entities of that type.
				You answer each desire as an infinite verb form that starts with the to particle.

				What does %s desire?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "desires");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * Obtains ISA relations relating the given class type and examples given by the LLM. The prompt must be in accordance. Called by populateKB_withExamples()
	 * 
	 * @param prompt
	 * @param classType
	 * @return
	 */
	public static ArrayList<StringEdge> getExamplesOf(String prompt, String classType) {
		System.out.printf("getting examples of %s\n", classType);
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();

		String reply = cleanReply(doRequest(prompt).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String newConcept = cleanLine(line);
			facts.add(new StringEdge(newConcept, classType, "isa"));
		}

		return facts;
	}

	/**
	 * Obtains an exhaustive list of well-known examples of the given class type, in the form of ISA relations. Also adds relations from the given class type.
	 * 
	 * @param classType
	 * @param kb
	 * @return
	 */
	public static ArrayList<StringEdge> getExamplesOfClass(String classType) {
		String prompt = PROMPT_INTRO + """
				Give an exhaustive list of well-known examples of %s.
				Do not explain those examples, only list their names.
				Answer each example as a noun phrase.""";
		String text = String.format(prompt.strip(), classType, classType);

		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			facts.add(new StringEdge(target, classType, "isa"));
		}

		return facts;
	}

	/**
	 * obtains the super classes of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getIsaClass(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about a hierarchical relationship indicating a superclass or a generalization.
				The question asks which types or super classes the given entity is. You answer each type as a noun phrase.
				You list one type per line. You answer all possible types that entity is.
				Be careful between generalization and specialization.
				The question asks about types that generalize the asked entity, not the specialization of the asked entity.

				What is %s?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "isa");
			facts.add(edge);
		}
		return facts;
	}

	/**
	 * obtains the concepts that the given entity is known for
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getKnownFor(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about what a given entity is known for.
				Answer the most likely or important subjects, topics, issues, ideas or proposals that entity is known for.
				Answer what the entity is known for in the form of an infinite verb that starts with the to particle.

				What is %s known for?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "knownfor");
			facts.add(edge);
		}
		return facts;
	}

	public static ArrayList<StringEdge> getLimitationsOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about the physical, mental or any other type of limitations of an entity.
				A limitation can also be a weakness, a lack of capacity, the inability or some restriction of the entity.
				You answer the most important entity's limitations.
				You answer each limitation of the entity as a noun phrase.

				What are the limitations of %s?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "limitedby");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the stuff that the given entity is made of
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getMadeOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityIsMadeOfSomething(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about what a given entity is made of. You list the most important materials.
				An entity can be made of a physical material, a form of matter, a substance, a solid,
				a chemical constitution or any other constituent that you may find correct.
				Answer each constituent of the entity as a noun phrase.

				What is %s made of?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "madeof");
			facts.add(edge);
		}
		return facts;
	}

	/**
	 * obtains the things that the given entity does not desire (dislikes)
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getNotDesires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasDesires(entity)) { // dislikes ~ desires
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about what an entity dislikes, what an entity has repulsion of, loathes or averts.
				You answer the most important dislikes of that entity.
				You only answer the dislikes that you are certain to be disliked by all entities of that type.
				You answer each dislike as an infinite verb form that starts with the to particle.


				What does %s dislike?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "notdesires");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the parts of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getPartOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasParts(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question is about an entity and its constituent parts.
				List the most well-known parts exclusive to that entity.
				Name only the parts that you are certain that belong to all entities of that type.
				If the entity is a person, answer with the specific parts of that human being.
				If the entity is an animal, answer with the parts of an example animal of its species.
				Otherwise, answer the parts of that entity that are appropriated.
				Most importantly, name each part as a noun phrase. Answer one part per line.

				What are the parts of %s?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(target, entity, "partof");
			facts.add(edge);
		}

		// get purposes for each part
		ArrayList<StringEdge> purposeFacts = new ArrayList<StringEdge>();
		for (StringEdge fact : facts) {
			String part = fact.getSource();
			ArrayList<StringEdge> partPurposes = getUsedTo(entity, part);
			purposeFacts.addAll(partPurposes);
		}
		facts.addAll(purposeFacts);
		return facts;
	}

	public static String getPhraseType(String phrase) {
		phrase = phrase.strip();

		// shortcuts
		if (nounPhrases_fromFile.contains(phrase))
			return "NP";
		// a single letter or number
		if (phrase.length() == 1)
			return "NP";
		if (phrase.startsWith("to "))
			return "VP";

		int numwords = VariousUtils.countWords(phrase);
		// single word with a number is very likely to be a NP
		if (numwords == 1) {
			if (VariousUtils.checkTextContainsNumber(phrase))
				return "NP";
		}

		String reply = cachedRawPhrases.get(phrase);
		if (reply == null) {
			// System.out.println("info: requesting getPhraseType for: " + phrase);
			String prompt = """
					You are a grammar classification program.
					You categorize the type of phrase structure of text given at the end.
					You do not explain your answer nor your reasoning.
					You only answer the phrase structure of the given text.
					If the given text is a single word, answer the part of speech of the text.
					This is the given text:
					- %s
					""";
//		String prompt = """
//				You classify the type of phrase structure for a given sentence.
//				There are various types of phrase structures: Sentence, Noun phrase,
//				Adverb phrase, Adjective phrase and Verb phrase.
//				Do not explain your classification or your answer.
//				What is the type of phrase of the following text?
//				%s""";
			String text = String.format(prompt.strip(), phrase);

			reply = doRequest(text).toLowerCase().strip();
			cachedRawPhrases.put(phrase, reply);
		}

		reply = reply.replaceAll("[\r\n]+", " "); // newlines
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
//		reply = reply.replaceAll("[\\[\\]]+", ""); // remove square brackets
//		reply = reply.replaceAll("[\\d]+", ""); // remove numbers
//		reply = reply.replaceAll("\\(([\\w]+)\\)", ""); // remove (vp) and similar tags

		if (reply.contains("verb") // --
				|| reply.contains("infinitive")// --
				|| reply.contains("particip")// --
				|| reply.contains("prepositional")// --
				|| reply.contains("imperative")// --
		)
			return "VP";

		if (reply.contains("noun") // --
				|| reply.contains("adjective")// --
				|| reply.contains("gerund")// --
				|| reply.contains("prepositional")// --
		)
			return "NP";

		// uncaught, return original
		// System.err.printf("UNPROCESSED PHRASE TYPE\t%s\t%s\n", phrase, reply);
		return reply;
	}

	/**
	 * obtains the requirements for the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getRequires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
//		if (!checkIfEntityHasRequirements(entity)) {
//			return facts;
//		}
		String prompt = PROMPT_INTRO + """
				The question are about an entity and its requirements.
				You answer the most important requirements.
				You only answer the requirements that you are certain to be required by all entities of that type.
				A requirement may be something needed for the entity’s functioning, something essential for its existence,
				or something mandatory for its purposes.
				You give one requirement per line.
				You answer each requirement as a noun phrase.

				What does %s require?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "requires");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the purposes of the given part (entity) that is part of a whole entity
	 * 
	 * @param whole
	 * @param part
	 * @return
	 */
	public static ArrayList<StringEdge> getUsedTo(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about an entity and its specific purposes or its specific functions.
				You answer with the most specific purposes of that entity.
				Answer each purpose as an infinite verb form that starts with the to particle.

				What are the purposes of %s?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "usedto");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the purposes of the given part (entity) that is part of a whole entity
	 * 
	 * @param whole
	 * @param part
	 * @return
	 */
	public static ArrayList<StringEdge> getUsedTo(String whole, String part) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about the purposes or functions of one of the parts of a containing entity.
				Answer each purpose of the part as an infinite verb form that starts with the to particle.

				What are the purposes of %s that is part of %s?""";
		String text = String.format(prompt.strip(), part, whole);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(part, target, "usedto");
			facts.add(edge);
		}

		return facts;
	}

	/**
	 * obtains the wholes that the given entity is part of
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getWhatIsPartOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = PROMPT_INTRO + """
				The question is about an entity and the greater whole that that entity is part of.
				Answer each whole as a noun phrase.
				You list one greater whole per line.

				What is %s part of?""";
		String text = String.format(prompt.strip(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "partof");
			facts.add(edge);
		}

		return facts;
	}

	public static boolean isRelationValid(StringEdge edge) throws IOException, URISyntaxException {
		/**
		 * relations to verify: atlocation capableof causes causesdesire createdby desires isa knownfor madeof notdesires partof requires usedto
		 */
		String source = edge.getSource();
		String target = edge.getTarget();
		String label = edge.getLabel();
		String cons_source = getPhraseType(source);
		String cons_target = getPhraseType(target);
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
			// name causesdesire VP
			if (cons_source.equals("NP") && cons_target.equals("VP"))
				return true;
			break;

		case "createdby": // checked
			// name createdby name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "desires": // checked
			// name desires VP
			if (cons_source.equals("NP") && cons_target.equals("VP"))
				return true;
			break;

		case "isa": // checked
			// name isa name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "knownfor": // checked
			// noun knownfor VP
			if (cons_source.equals("NP") && cons_target.equals("VP"))
				return true;
			break;

		case "madeof": // checked
			// name madeof name
			if (cons_source.equals("NP") && cons_target.equals("NP"))
				return true;
			break;

		case "notdesires": // checked
			// name notdesires VP
			if (cons_source.equals("NP") && cons_target.equals("VP"))
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
			// <> usedto VP
			if (cons_target.equals("VP"))
				return true;
			break;

		}
		System.out.printf("%s\t%s,%s,%s\n", edge, cons_source, label, cons_target);
		return false;
	}

	public static ArrayList<StringEdge> old_getPartsAndPurpose(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasParts(entity)) {
			return facts;
		}
		String prompt = """
				Your answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph.
				The question is about an entity, that entity's parts and the function that those parts have in the whole entity. You answer as many parts as possible. For each part, you list all its purposes. A purpose is answered with a single verb followed by the receiving entity. Answer each part with a noun. For each line of your answer, list one part followed by all the purposes of that part.
				What are the parts and their purpose of church?""";
		String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.strip(), article, entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String[] tokens = line.split(":");
			String part = tokens[0];
			StringEdge partof = new StringEdge(part, entity, "partof");
			facts.add(partof);
			String[] purposes = tokens[1].split(",");
			for (String purpose : purposes) {
				purpose = purpose.strip();
				StringEdge purposeFact = new StringEdge(part, purpose, "usedto");
				facts.add(purposeFact);
			}
			// System.lineSeparator();
		}
		return facts;
	}

	private static String removeTextAfterParentheses(String concept) {
		int p0 = concept.indexOf("(");
		int p1 = concept.indexOf(")");
		if (p0 == -1 || p1 == -1) {
			return concept;
		} else {
//			String before = concept.substring(0, p0 - 1);
//			String between = concept.substring(p0 + 1, p1);
//			String after = concept.substring(p1 + 1);
//			if(after.length()>1) {
//				System.out.println(concept);
//			}
			return concept.substring(0, p0);
		}
	}

	public static void saveCaches() {
		try {
			cachedConceptIsLifeform.save();
			cachedConceptIsPlural.save();
			cachedConceptIsEntity.save();
			cachedConceptIsSuperClass.save();
			cachedPlural2Singular.save();
			cachedConceptHasExamples.save();
			cachedRawPhrases.save();
			cachedVP_to_NP.save();
			cachedNP_to_VP.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static BooleanArrayList verifyISA_facts(Collection<StringEdge> edges) throws IOException, URISyntaxException {
		final String base_prompt = """
				You verify multiple facts that are given by the user. You answer with each fact followed by yes or no according to your verification.
				If there are typos or spelling errors in the fact, you answer no.
				If according to your knowledge the fact is wrong, you answer no.
				If the given fact is unknown, you answer no. Otherwise you answer yes.
				Each fact is separated by the single forward slash symbol /
				The multiple facts start here:
					""";

		String facts = "";
		for (StringEdge edge : edges) {
			assert edge.getLabel().equals("isa");
			// source ISA target -> concept ISA superclass
			String concept = edge.getSource();
			String superclass = edge.getTarget();

			String article = IndefiniteArticle.get(superclass);
			String text = String.format("%s is %s %s /", concept, article, superclass);
			facts += text + "\n";
		}
		String text = base_prompt + facts;

		String reply = "";
		reply = doRequest(text).toLowerCase().strip();

		String reply_original = reply;

//		System.out.println(reply);
		reply = reply.replace("\\", "/"); // you never know...
		reply = reply.replace("/", " ");
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replace(" \n", "\n"); // empty lines
		reply = reply.replace("|", "");
		reply = reply.strip();

		BooleanArrayList facts_evaluations = new BooleanArrayList();
		// check answer format
		int num_newlines = VariousUtils.countCharOccurences(reply, '\n');

		if (num_newlines == edges.size() - 1) {
			StringTokenizer st = new StringTokenizer(reply, "\n");
			while (st.hasMoreTokens()) {
				String line = st.nextToken();
				// lagidium ahuacaense is a eukaryote yes
				// lagidium ahuacaense is a eukaryote no
				String valid = line.substring(line.lastIndexOf(' ') + 1);
				if (valid.equals("yes")) {
					facts_evaluations.add(true);
				} else if (valid.equals("no")) {
					facts_evaluations.add(false);
				} else {
					System.err.printf("unrecognized token|%s|\n", valid);
				}
			}
		} else {
			// no yes yes yes no yes no yes yes no
			String[] tokens = reply.split(" ");
			if (tokens.length == edges.size()) {
				for (String token : tokens) {
					if (token.equals("yes")) {
						facts_evaluations.add(true);
					} else if (token.equals("no")) {
						facts_evaluations.add(false);
					} else {
						System.err.printf("unrecognized token|%s|\n", token);
					}
				}
			}
		}
		if (facts_evaluations.size() != edges.size()) {
			System.err.printf("unrecognized output|%s|\nfor input\n%s\n", reply_original, text);
		}
		return facts_evaluations;
	}
}
