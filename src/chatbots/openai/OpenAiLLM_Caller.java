package chatbots.openai;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import linguistics.GrammarUtilsCoreNLP;
import stream.ParallelConsumer;
import utils.InvocationsPerMinuteTracker;
import utils.VariousUtils;

public class OpenAiLLM_Caller {

	public static final String llm_model = "gpt-4o-mini";
	private static String api_key;
	private static boolean initialized = false;
	private static SimpleOpenAI openAI;
	private static ChatCompletions chatCompletions;

	private static void init() {
		if (initialized)
			return;
		try {
			api_key = VariousUtils.readFile("data/openai_api_key.txt");
		} catch (IOException e) {
			// no api key, forget it
			e.printStackTrace();
			System.exit(-1);
		}
		openAI = SimpleOpenAI.builder().apiKey(api_key).build();
		chatCompletions = openAI.chatCompletions();
		initialized = true;
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
		init();
		while (true) {
			try {
				InvocationsPerMinuteTracker.printInvocationsPerMinute("OpenAI call");
				ChatRequest chatRequest = ChatRequest.builder().model(llm_model).message(UserMessage.of(prompt)).temperature(0.000).maxCompletionTokens(512)
						.frequencyPenalty(0.05).presencePenalty(0.05).build();
				CompletableFuture<Chat> futureChat = chatCompletions.create(chatRequest);
				Chat chatResponse = futureChat.join();
				String firstContent = chatResponse.firstContent();
//		System.out.println(firstContent);
				return firstContent;
			} catch (CompletionException e) {
				System.err.println(e.getMessage());
				// HTTP interaction failed: server returned a 429 response status.
				// 429 error means that query rate limit has been Exceeded
				if (e.getMessage().contains("429")) {
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static String cleanLine(String line) {
		line = line.trim();
		line = line.replace(".", ""); // remove dots
		line = line.replaceAll("^[\\s\t]*[-]+[\\s\t]*", ""); // start space* - space* (multiple space)
		line = line.replaceAll("[ ]*-[ ]*", " "); // space-space -> space
		// number followed by text
		String test = line.replaceAll("^[\\d]+[ \\t]+[\\w ]+", "");
		if (test.isEmpty()) {
			line = line.substring(line.indexOf(' ') + 1);
		}
		line = line.replaceAll("\\s*\\(.+\\).*$", ""); // text between parentheses and text that follows until the end of string
		String target = GrammarUtilsCoreNLP.preprocessConcept(line);
		target = target.replace("desire for ", "");
		target = target.replace("desire to ", "");
		// target = target.replace("- ", "");
		return target;
	}

	public static String cleanReply(String reply) {
		reply = reply.trim();
		reply = reply.replaceAll("\\s*[,:]+\\s*", " "); // ...,... -> ' ' no commas can go into here, because of the CSV format
		reply = reply.replaceAll("[ \t]+", " ");// multiple whitespace -> one space
		reply = reply.replace(".", ""); // remove dots
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		return reply;
	}

	public static boolean checkIfEntityHasRequirements(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s require anything or %s have pre-conditions?""";
		String text = String.format(prompt.trim(), entity, entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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
		String text = String.format(prompt.trim(), entity);
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

	public static boolean checkIfEntityCreates(String entity) {
		String prompt = """
				Evaluate if the following question is logical or if the question makes sense. Do not explain your reasoning nor your answer. Answer strictly yes or no.
				Did in the past or does in the present %s create anything?""";
		String text = String.format(prompt.trim(), entity);
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
		String prompt = """
				Your answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reason or your answer. Do not enumerate your answer.
				The question is about the desires that an entity elicits in other entities. A desire may be conscious impulses towards something that promises enjoyment or satisfaction in its attainment, longing or craving or a sudden spontaneous inclination or incitement to some usually unpremeditated action. You list the most important desires. You only list the desires that all entities of that type elicit on other entities. You list each elicited desire with a verb phrase. You list one elicited desire per line. Do not fancy format your answer.
				What desires does %s elicit in someone?""";
		String text = String.format(prompt.trim(), entity);
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

	/**
	 * obtains the requirements for the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getRequires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasRequirements(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The questions are about an entity and its requirements. You answer the most important requirements. You only answer the requirements that you are certain to be required by all entities of that type. A requirement may be something required for the entity’s functioning, something required for its existence, or something required for its purposes. You give one requirement per line. You answer a requirement as a noun phrase.
				What does %s require?""";
		String text = String.format(prompt.trim(), entity);
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
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about the impact that the given entity has on other entities, as well as its consequences or what effects the entity has on other entities. You list the most consequences of that entity. You only answer the consequences that you are certain to be caused by all entities of that type. A consequence may be something that the entity causes either by existing or by interacting with something, such as occasions or events. Do not answer the purposes, do not list its objectives nor the goals of that entity. The purpose of an entity is not the same as its consequences nor the same as its impacts. Answer each consequence of the entity as a noun phrase.
				What consequences does %s cause?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		String strip = doRequest(text).toLowerCase().strip();
		reply = cleanReply(strip);

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			// remove redundant words
			// remove entity self reference

			// rever isto
//				int ix = target.indexOf(entity);
//				if (ix >= 0) {
//					target = target.substring(ix + entity.length() + 1);
//				}

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
	 * obtains the stuff that the given entity is made of
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getMadeOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityIsMadeOfSomething(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about what a given entity is made of. You list the most important materials. An entity can be made of a physical material, from a form of matter, made from a substance, from a solid or the given entity can be made of a chemical constitution. Answer each material as a noun phrase
				What is %s made of?""";
		String text = String.format(prompt.trim(), entity);
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
		if (!checkIfEntityHasDesires(entity)) { // dislikes ~ desires
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The questions are about an entity and what that entity dislikes, what it has repulsion of, it loathes or it averts. You answer the most important dislikes by that entity. You only answer the dislikes that you are certain to be disliked by all entities of that type. You answer a dislike as a noun phrase.
				What does %s dislike?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains the desires of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getDesires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasDesires(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The questions are about an entity and what that entity desires, what that entity wishes, what that entity wants or what that entity hopes for. You answer the most important desires by that entity. You only answer the desires that you are certain to be desired by all entities of that type. You answer a desire as a noun phrase.
				What does %s desire?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains the capabilities of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCapableOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasCapabilities(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about what an entity is capable of or able to. You answer the most important and specific entity’s capabilities. You answer each capability of that entity as an action verb. Do not format your answer. You list one capability per line. Do not enumerate your answer.
				What is %s capable of?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

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
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The questions are about an entity and the greater whole that the entity is part of. Answer each whole as a noun phrase. You list one greater whole per line. Do not enumerate your answer.
				What is %s part of?""";
		String text = String.format(prompt.trim(), entity);
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

	/**
	 * obtains the parts of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getPartOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasParts(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about an entity and its constituent parts. List the most well-known parts exclusive to that entity. Name only the parts that you are certain that belong to all entities of that type. Most importantly, a part is answered as a noun phrase. You list one part per line. If the entity is a person, answer with the parts of the human being. If the entity is an animal, answer with the parts of an animal of its species.
				What are the parts of %s?""";
		String text = String.format(prompt.trim(), entity);
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
			ArrayList<StringEdge> partPurposes = getUsedFor(entity, part);
			purposeFacts.addAll(partPurposes);
			System.lineSeparator();
		}
		facts.addAll(purposeFacts);
		return facts;
	}

	/**
	 * obtains the purposes of the given part (entity) that is part of a whole entity
	 * 
	 * @param whole
	 * @param part
	 * @return
	 */
	public static ArrayList<StringEdge> getUsedFor(String whole, String part) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about an entity and the purposes or functions of one of its parts. Answer the purpose with a transitive verb. You list one purpose per line. Do not enumerate your answer.
				What are the purposes of the part %s that belongs to %s?""";
		String text = String.format(prompt.trim(), part, whole);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(part, target, "usedfor");
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
	public static ArrayList<StringEdge> getUsedFor(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about an entity and its specific purposes or its specific functions. You list the most specific purposes of the given entity. Answer each purpose with a transitive verb. List one purpose per line.
				What are the purposes of %s?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		reply = cleanReply(doRequest(text).toLowerCase().strip());

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			StringEdge edge = new StringEdge(entity, target, "usedfor");
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
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The questions are about an entity and what it is known for. Answer what it is known for in the form of a verb phrase. A verb phrase is composed of a verb and a noun. Only answer the most likely or important subjects that entity is known for. List one known fact per line. Do not enumerate your answer.
				What is %s known for?""";
		String text = String.format(prompt.trim(), entity);
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

	/**
	 * obtains the entities that created or create the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCreatedBy(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasCreator(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about who or what created a given entity. You answer each creator as a noun phrase or with its name. A creator may be a person, a collective, a company or any another type of entity. If the asked entity is a person or an animal, you name its parents or progenitors. Answer all possible creators.
				Who or what created %s?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains the super classes of the given entity
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getIsaClass(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about a hierarchical relationship indicating a superclass or a generalization.
				The question asks which types or super classes the given entity is. You answer each type as a noun phrase. you list one type per line. You answer all possible types that entity is. Be careful between generalization and specialization. The question asks about types that generalize the asked entity. Do not enumerate your answer.
				What is %s?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains entities created by the given entity (or entities that the given entity creates)
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getCreates(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityCreates(entity)) {
			return facts;
		}
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about what a given entity can create or originate. You answer each creation as a noun phrase or with its name. A creation may be a person, a collective, a single entity, a company or any another type of entity. If the asked entity is a person or an animal, you name its successors or children. Answer all possible creations.
				Who or what does %s create?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains the places where the entity is located at
	 * 
	 * @param entity
	 * @return
	 */
	public static ArrayList<StringEdge> getAtLocation(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		//
		//
		//
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				The question is about the locations or places that a given entity is usually at or located at. You list the most important locations. A location can be a common noun or a proper noun, such as a city, village, country, etc. Answer each location as a noun phrase.
				Where is %s located?""";
		String text = String.format(prompt.trim(), entity);
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
	 * obtains an exhaustive list of well-known examples of the given class type
	 * 
	 * @param classType
	 * @return
	 */
	public static ArrayList<StringEdge> getExamplesOfClass(String classType) {
		//
		String prompt = """
				All your knowledge is in English. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. You answer in simple, unformatted text. Do not fancy format your output. When there are multiple possibilities, you give one answer per line. Do not enumerate your answer.
				Give an exhaustive list of well-known %s. Do not explain those %s, only list their names. Answer each name as a noun phrase.""";
		String text = String.format(prompt.trim(), classType, classType);

		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String reply = "";
		reply = doRequest(text).toLowerCase().strip();
		reply = reply.replace(", ", ","); // you never know...
		reply = reply.replace(".", "");
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replace(" \n", "\n"); // empty lines
		reply = reply.replaceAll("\\([\\w ]+\\)", ""); // text between parentheses

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			facts.add(new StringEdge(target, classType, "isa"));
		}

		return facts;
	}

	/**
	 * obtains concept examples of the given class type. the prompt must be in accordance
	 * 
	 * @param prompt
	 * @param classType
	 * @return
	 */
	public static ArrayList<StringEdge> getExamplesOf(String prompt, String classType) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String reply = "";
		reply = doRequest(prompt).toLowerCase().strip();
		reply = reply.replace(", ", ","); // you never know...
		reply = reply.replace(".", "");
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replace(" \n", "\n"); // empty lines
		reply = reply.replaceAll("\\([\\w ]+\\)", ""); // text between parentheses

		String[] lines = reply.split("\n");
		for (String line : lines) {
			String target = cleanLine(line);
			facts.add(new StringEdge(target, classType, "isa"));
		}

		return facts;
	}

	public static String getPhraseType(String phrase) {
		//
		String prompt = """
				Do not explain your reasoning. Ignore case sensitivity. Is the following text a noun phrase or a verb phrase?
				%s""";
		String text = String.format(prompt.trim(), phrase, phrase);

		String reply = "";
		reply = doRequest(text).toLowerCase().strip();
		reply = reply.replace(", ", ","); // you never know...
		reply = reply.replace(".", "");
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replace(" \n", "\n"); // empty lines
		reply = reply.replaceAll("\\([\\w ]+\\)", ""); // text between parentheses

		return reply;
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
		String text = String.format(prompt.trim(), article, entity);
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
				purpose = purpose.trim();
				StringEdge purposeFact = new StringEdge(part, purpose, "usedfor");
				facts.add(purposeFact);
			}
			// System.lineSeparator();
		}
		return facts;
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
			System.lineSeparator();
		}
		if (facts_evaluations.size() != edges.size()) {
			System.err.printf("unrecognized output|%s|\nfor input\n%s\n", reply_original, text);
		}
		return facts_evaluations;
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
		final long pauseDurationMillis = 1 * 11 * 1000;
		final int numThreads = 2;

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

	public static void runGetStuff(StringGraph inputSpace) throws InterruptedException, IOException {
		List<String> initialConcepts = VariousUtils.readFileRows("newconcepts.txt");

		int numThreads = 20;
		int blockSize = 1000;

		int numConcepts = initialConcepts.size();
		if (numThreads > numConcepts) {
			numThreads = numConcepts;
		}

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

			ParallelConsumer<String> pc = new ParallelConsumer<>(numThreads);
			pc.parallelForEach(openSetBatch, concept -> {
				concept = cleanLine(concept);
				boolean conceptClosed = true;
				{
					rwLock.readLock().lock();
					conceptClosed = closedSet.contains(concept);
					rwLock.readLock().unlock();
				}
				if (concept.length() <= 32 && (VariousUtils.countCharOccurences(concept, ' ') + 1) <= 2 && !conceptClosed) {

					try {
						ArrayList<StringEdge> localEdges = new ArrayList<StringEdge>();
						localEdges.addAll(OpenAiLLM_Caller.getCapableOf(concept));
						localEdges.addAll(OpenAiLLM_Caller.getCauses(concept));
						localEdges.addAll(OpenAiLLM_Caller.getCausesDesire(concept));
						localEdges.addAll(OpenAiLLM_Caller.getCreatedBy(concept));
						localEdges.addAll(OpenAiLLM_Caller.getCreates(concept));
						localEdges.addAll(OpenAiLLM_Caller.getDesires(concept));
						localEdges.addAll(OpenAiLLM_Caller.getIsaClass(concept));
						localEdges.addAll(OpenAiLLM_Caller.getKnownFor(concept));
						localEdges.addAll(OpenAiLLM_Caller.getMadeOf(concept));
						localEdges.addAll(OpenAiLLM_Caller.getNotDesires(concept));
						localEdges.addAll(OpenAiLLM_Caller.getPartOf(concept));
						localEdges.addAll(OpenAiLLM_Caller.getRequires(concept));
						localEdges.addAll(OpenAiLLM_Caller.getUsedFor(concept));
						localEdges.addAll(OpenAiLLM_Caller.getWhatIsPartOf(concept));
						localEdges.addAll(OpenAiLLM_Caller.getAtLocation(concept));
						lock.lock();
						{
							facts.addAll(localEdges);
						}
						lock.unlock();
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
			System.err.println("round " + counter + " done LLM calls, processing CoreNLP...");

			// collect into openConcepts the concepts from facts that are NP and not in the closed set
			{
				HashSet<String> conceptsToAdd = new HashSet<String>();
				for (StringEdge fact : facts) {
					conceptsToAdd.add(fact.getSource());
					conceptsToAdd.add(fact.getTarget());
				}
				conceptsToAdd.parallelStream().forEach(concept -> {
					// do not explore lengthy concepts or with many words
					if (concept.length() <= 32 && (VariousUtils.countCharOccurences(concept, ' ') + 1) <= 3) {
						if (!closedSet.contains(concept)) {
							String phraseType = GrammarUtilsCoreNLP.getClassificationFromCoreNLP_raw(concept);
							if (phraseType.equals("NP")) {
								lock.lock();
								{
									openSet.add(concept);
								}
								lock.unlock();
							}
						}
					} else {
						// System.err.printf("2 skipping concept %s\n", concept);
					}
				});
			}

			System.err.printf("expanded %d concepts, %d new facts discovered, %d new concepts, %d total explored concepts\n", previousSize, facts.size(),
					openSet.size(), closedSet.size());
			VariousUtils.writeFile("newconcepts" + counter + ".txt", openSet);

			counter++;
		}
	}

	public static void runGetExamples() {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an exhaustive list of well-known comic characters. Do not explain those comic characters, only list their names. Answer each name as a noun phrase.
						""",
				"comic character")));

		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an exhaustive list of well-known jobs or occupations. Do not explain those jobs, only list their names. Answer each name as a noun phrase.
						""",
				"occupation")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List all proven and demonstrated elementary particles in physics. Do not explain those particles, only list their names. Answer each elementary particle as a noun phrase. Do not list hypothetical or unproven particles.
						""",
				"elementary particle")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the thirty most important and well-known electronic components used in electronics. Do not explain those components, only list their names. Answer each name as a noun phrase. Do not list hypothetical or unproven components.
						""",
				"electronic component")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List all types of audio equipment used in audio or in music. Do not explain those equipments , only list their names. Answer each audio equipment as a noun phrase.
						""",
				"audio equipment")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an exhaustive list of organs present in vertebrate animals. Do not explain those organs, only list their names. Answer each organ as a noun phrase.
						""",
				"organ")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an exhaustive list of tools used by tradesperson in their daily work. Do not explain those tools , only list their names. Answer each tool as a noun phrase.
						""",
				"tool")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an exhaustive list of medication types. Do not explain those medications, only list their names. Answer each medication as a noun phrase.
						""",
				"drug")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give a list of cosmetic types. Do not explain those types, only list their names. Answer each cosmetic type type as a noun phrase.
						""",
				"cosmetic")));
		// --------------
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Given a comprehensive list of musical instruments. Do not explain those instruments, only list their names. Answer each musical instrument as a noun phrase.
						""",
				"musical instrument")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						Give an comprehensive list of items or products sold in a supermarket. Do not explain those products , only list their names. Answer each product as a noun phrase.
						""",
				"merchandise")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List all types of weapons usable by a single person. Do not explain those weapons, only list their names. Answer each weapon as a noun phrase.
						""",
				"weapon")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List common furniture that is present in a home or in a building. Do not explain those furniture, only list their names. Answer each furniture as a noun phrase.
						""",
				"furniture")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List common hardware components of a computer. Do not explain those component, only list their names. Answer each hardware component as a noun phrase.
						""",
				"hardware component")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 types of celestial objects of the universe. Do not explain those celestial objects, only list their names. Do not answer hypothetical or unproven celestial objects. Answer each celestial object as a noun phrase.
						""",
				"celestial object")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 50 celestial objects that can be seen with a telescope. Do not explain those celestial objects, only list their names. Answer each celestial object as a noun phrase.
						""",
				"celestial object")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most popular sports. Include group sports and individual sports. Do not explain those sports, only list their names. Answer each sport as a noun phrase.
						""",
				"sport")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most popular video games of all time. Include console and computer games. Do not explain those video games, only list their names. Answer each video game as a noun phrase.
						""",
				"video game")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most popular table games. Do not explain those table games, only list their names. Answer each table game as a noun phrase.
						""",
				"table game")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common illnesses. Do not explain those illnesses, only list their names. Answer each illness as a noun phrase.
						""",
				"illness")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common diseases. Do not explain those diseases, only list their names. Answer each disease as a noun phrase.
						""",
				"disease")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common viruses. Do not explain those viruses, only list their names. Answer each virus as a noun phrase.
						""",
				"virus")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common bacteria. Do not explain those bacteria, only list their names. Answer each bacteria as a noun phrase.
						""",
				"bacteria")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common mushrooms. Do not explain those mushrooms, only list their names. Answer each mushroom as a noun phrase.
						""",
				"mushroom")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common plants in a garden. Do not explain those plants , only list their names. Answer each plant as a noun phrase.
						""",
				"plant")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 20 most common mental disorders. Do not explain those mental disorders, only list their names. Answer each mental disorder as a noun phrase.
						""",
				"mental disorder")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 30 most common atomic elements. Do not explain those atomic elements, only list their names. Answer each atomic element as a noun phrase.
						""",
				"atomic element")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 30 most common chemical molecules present in the earth. Do not explain those chemical molecules, only list their names. Answer each chemical molecule as a noun phrase.
						""",
				"cosmetic")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List body parts of complex animals. Do not explain those body parts, only list their names. Answer each body part as a noun phrase.
						""",
				"body part")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 50 most famous inventions. Do not explain those inventions, only list their names. Answer each invention as a noun phrase.
						""",
				"invention")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 30 most famous stories or epic poems. Focus on classical or ancient stories. Do not explain those stories, only list their names. Do not include the story's author. Answer each story as a noun phrase.
						""",
				"novel")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 30 most famous novels. List novels of the last century. Do not explain those novels, only list their names. Do not include the novel's author. Answer each novel as a noun phrase.
						""",
				"novel")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List the 50 most famous fictional characters of classical novels and poems. Focus on classical or ancient characters. Do not explain those fictional characters, only list their names. Answer each fictional character as a noun phrase.
						""",
				"fictional character")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most well-known characters of star trek. Do not explain those characters, only list their names. Answer each character as a noun phrase.
						""",
				"fictional character")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most well-known characters of star wars. Do not explain those characters, only list their names. Answer each character as a noun phrase.
						""",
				"fictional character")));
		facts.addAll(getAllRelations(getExamplesOf(
				"""
						You answer to a single question in non-formatted text. You answer with simple words that will be interpreted by an expert system and stored in a knowledge graph. When there are multiple answer possibilities, you give one answer per line. You do not explain your reasoning or your answer. Do not enumerate your answer.
						List 30 of the most common home appliances. Do not explain those home appliances, only list their names. Answer each home appliance as a noun phrase.
						""",
				"home appliance")));
	}

	/**
	 * receives a list of ISA facts and populates each fact source with relations from a LLM
	 * 
	 * @param examples
	 * @return
	 */
	private static ArrayList<StringEdge> getAllRelations(ArrayList<StringEdge> examples) {

		ReentrantLock lock = new ReentrantLock();
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		ParallelConsumer<StringEdge> pc = new ParallelConsumer<>(8);
		try {
			pc.parallelForEach(examples, example -> {
				ArrayList<StringEdge> allRelations = getAllRelations(example);
				lock.lock();
				{
					facts.addAll(allRelations);
				}
				lock.unlock();
				System.out.println(example);
			});
			pc.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return facts;
	}

	private static ArrayList<StringEdge> getAllRelations(StringEdge edge) {
		assert edge.getLabel().equals("isa");

		// formats as concept (type)
		String concept = edge.getSource();
		String type = edge.getTarget();
		String conceptWithContext = String.format("%s (%s)", concept, type);
		ArrayList<StringEdge> localEdges = new ArrayList<StringEdge>();
		localEdges.addAll(OpenAiLLM_Caller.getCapableOf(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getCauses(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getCausesDesire(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getCreatedBy(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getCreates(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getDesires(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getIsaClass(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getKnownFor(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getMadeOf(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getNotDesires(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getPartOf(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getRequires(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getUsedFor(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getWhatIsPartOf(conceptWithContext));
		localEdges.addAll(OpenAiLLM_Caller.getAtLocation(conceptWithContext));

		ArrayList<StringEdge> tempEdges = new ArrayList<StringEdge>();
		for (StringEdge t_edge : localEdges) {
			tempEdges.add(t_edge.replaceSourceOrTarget(conceptWithContext, concept));
		}

		return tempEdges;
	}

}
