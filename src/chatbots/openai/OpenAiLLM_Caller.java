package chatbots.openai;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import linguistics.GrammarUtilsCoreNLP;
import utils.VariousUtils;

public class OpenAiLLM_Caller {

	public static final String llm_model = "gpt-4o-mini";
	private static String api_key;
	private static boolean initialized = false;
	private static SimpleOpenAI openAI;
	private static ChatCompletions chatCompletions;
	private static AtomicBoolean rateLimitExceeded = new AtomicBoolean(false);

	private static void init() throws IOException, URISyntaxException {
		if (initialized)
			return;
		api_key = VariousUtils.readFile("data/openai_api_key.txt");
		openAI = SimpleOpenAI.builder().apiKey(api_key).build();
		chatCompletions = openAI.chatCompletions();
		initialized = true;
	}

	public static String doRequest(String prompt) throws IOException, URISyntaxException {
		init();

		ChatRequest chatRequest = ChatRequest.builder().model(llm_model).message(UserMessage.of(prompt)).temperature(0.0).maxCompletionTokens(512).build();
		CompletableFuture<Chat> futureChat = chatCompletions.create(chatRequest);
		Chat chatResponse = futureChat.join();
		String firstContent = chatResponse.firstContent();
//		System.out.println(firstContent);
		return firstContent;
	}

	public static boolean checkIfEntityHasRequirements(String entity) {
		String prompt = """
				You are a knowledge base that answers questions made by an expert system.
				All your knowledge is in English. You have a comprehensive ontology and
				knowledge base that spans the basic concepts and rules about how the world works.
				Be as specific as possible and do not generalize. Answer the following question with yes or no.
				does %s require anything or %s have pre-conditions?""";
		// String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.trim(), entity, entity);
		String reply = "";
		try {
			reply = doRequest(text).toLowerCase().strip();
			System.lineSeparator();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}

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
				You are a knowledge base that answers questions made by an expert system.
				All your knowledge is in English. You have a comprehensive ontology and
				knowledge base that spans the basic concepts and rules about how the world works.
				Be as specific as possible and do not generalize. Answer the following question with yes or no.
				does %s have desires?""";
		// String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.trim(), entity);
		String reply = "";
		try {
			reply = doRequest(text).toLowerCase().strip();
			System.lineSeparator();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasMotivesOrGoals(String entity) throws IOException, URISyntaxException {
		String prompt = """
				You are a knowledge base that answers questions made by an expert system.
				All your knowledge is in English. You have a comprehensive ontology and
				knowledge base that spans the basic concepts and rules about how the world works.
				Be as specific as possible and do not generalize. Answer the following question with yes or no.
				does %s have goals to achieve?""";
		// String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.trim(), entity);
		String reply = "";
		try {
			reply = doRequest(text).toLowerCase().strip();
			System.lineSeparator();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static boolean checkIfEntityHasNotableIdeas(String entity) throws IOException, URISyntaxException {
		String prompt = "is %s known to have notable ideas? answer yes or no.";
		String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.trim(), article, entity);
		String reply = "";
		try {
			reply = doRequest(text).toLowerCase().strip();
			System.lineSeparator();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}

		if (reply.startsWith("yes")) {
			return true;
		} else if (reply.startsWith("no")) {
			return false;
		} else
			System.err.println("unknown answer:" + reply + " for query\n" + text);
		return false;
	}

	public static ArrayList<StringEdge> getDesiresCaused(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities. Be specific about the question. Do not generalize.
				The questions are about a *generic entity* and what *desires* it might *elicit* in someone. You answer the most important desires by that
				entity in the form of a **verb**. A desire may be conscious impulses towards something that promises enjoyment or satisfaction in its attainment, longing or
				craving or a sudden spontaneous inclination or incitement to some usually unpremeditated action.
				You answer each elicited desire with a **verb**. You answer one elicited desire per line. Do not fancy format your answer.
				Which desires does %s elicit in someone?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines
			reply = reply.replaceAll("\\([\\w ]+\\)", "");

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = preprocessConcept(line);
				StringEdge edge = new StringEdge(entity, target, "causesdesire");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getRequirements(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasRequirements(entity)) {
			return facts;
		}
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the
				world works. You do not explain your answer nor your reasoning. You answer all possibilities. Be specific. Do not generalize.
				The questions are about a generic entity and its requirements. You answer the most important requirements.
				A requirement may be something required for the entity’s functioning, something required
				for its existence, or something required for its purpose. You answer each requirement as a noun or a
				noun phrase. You give one answer per line. Do not fancy format your answer.
				What does %s require?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines
			reply = reply.replaceAll("\\([\\w ]+\\)", "");

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = preprocessConcept(line);
				StringEdge edge = new StringEdge(entity, target, "requires");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	private static String preprocessConcept(String line) {
		String target = GrammarUtilsCoreNLP.preprocessConcept(line.strip());
		target = target.replace("desire for ", "");
		target = target.replace("desire to ", "");
		return target;
	}

	public static ArrayList<StringEdge> getCauses(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English. You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works. You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible. You do not generalize. Your answer in simple, unformatted text. Do not fancy format your output. Your answers are to be easily stored in a knowledge graph. When there are multiple possibilities, you give one answer per line.
				The questions are about a generic entity and what it causes or its consequences. You answer the most important causes by that entity. A cause may be something that the entity causes either by existing or by interacting with something, such as occasions or events. Most importantly, a cause is answered as a transitive verb. A transitive verb is a verb followed by a direct object, the receiver of the cause.
				What does %s cause?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines
			reply = reply.replaceAll("\\([\\w ]+\\)", "");

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = preprocessConcept(line);
				int ix = target.indexOf(entity);
				if (ix >= 0) {
					target = target.substring(ix + entity.length() + 1);
				}
				ix = target.indexOf("cause ");
				if (ix >= 0) {
					target = target.substring(ix + 6);
				}
				StringEdge edge = new StringEdge(entity, target, "causes");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getMadeOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in American English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities. You are as specific as possible about
				the question asked. You do not generalize.
				The questions made to you are about an entity and what materials or substances it is made of.
				You answer one material per line. Do not format your answer.
				What is %s made of?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines
			reply = reply.replaceAll("\\([\\w ]+\\)", "");

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = line.strip();
				StringEdge edge = new StringEdge(entity, target, "madeof");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getEntityDislikes(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasDesires(entity)) {
			return facts;
		}
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities.
				Try to be as specific as possible. Do not generalize.
				The questions are about an entity and what that entity might dislike, has repulsion of, loathes or averts.
				You answer each dislike as a noun in the singular or an action verb. You list one dislike per line. Do not format your answer.
				What does %s dislike?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = line.strip();
				StringEdge edge = new StringEdge(entity, target, "notdesires");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getEntityDesires(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		if (!checkIfEntityHasDesires(entity)) {
			return facts;
		}
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities. Try to be as specific as possible.
				Do not generalize.
				The questions are about an entity and what that entity might desire.
				You answer each desire as a noun in the singular or an action verb. You list one desire per line.
				Do not format your answer.
				What does %s desire?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = line.strip();
				StringEdge edge = new StringEdge(entity, target, "desires");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getCapableOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities.
				Try to be as specific as possible and do not generalize.
				The questions are about an entity and what that entity is capable of or able to.
				You answer the most important and specific capabilities. You answer each capability of that entity as an action verb.
				Do not format your answer. You list one capability per line.
				What is %s capable of?""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = line.strip();
				StringEdge edge = new StringEdge(entity, target, "capableof");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getWhatIsPartOf(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities.
				Try to be as specific as possible and do not generalize.
				The questions are about an entity and what it may be part of. You answer each thing that entity is part of as noun.
				You list one thing that entity is part of per line.	Do not format your answer.
				What is %s part of?	""";
		String text = String.format(prompt.trim(), entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines

			String[] lines = reply.split("\n");
			for (String line : lines) {
				String target = line.strip();
				StringEdge edge = new StringEdge(entity, target, "partof");
				facts.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}
		return facts;
	}

	public static ArrayList<StringEdge> getPartsAndPurpose(String entity) {
		ArrayList<StringEdge> facts = new ArrayList<StringEdge>();
		String prompt = """
				You are a knowledge base that answers questions made by an expert system. All your knowledge is in American English.
				You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works.
				You do not explain your answer nor your reasoning. You answer all possibilities. Be as specific as possible. Do not generalize.

				The questions made to you are about a generic entity and its constituent parts. You answer with as many parts of the entity as possible.
				You answer each part as a noun in the singular form. For each part, you answer as many purposes for that part as possible.
				Answer each purpose as verb object. You answer each part in one line followed by the various purposes of that part. Do not format your answer.

				What are the parts and their purpose of %s %s?
								""";
		String article = IndefiniteArticle.get(entity);
		String text = String.format(prompt.trim(), article, entity);
		String reply = "";

		try {
			reply = doRequest(text).toLowerCase().strip();
			reply = reply.replace(", ", ","); // you never know...
			reply = reply.replace(".", "");
			reply = reply.replace("\t", " "); // tabs -> spaces
			reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
			reply = reply.replace("\r\n", "\n"); // windows -> unix newline
			reply = reply.replaceAll("[\n]+", "\n"); // empty lines
			reply = reply.replace(" \n", "\n"); // empty lines

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
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
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
		try {
			reply = doRequest(text).toLowerCase().strip();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (CompletionException e) {
			System.err.println(e.getMessage());
			// HTTP interaction failed: server returned a 429 response status.
			// 429 error means that query rate limit has been Exceeded
			if (e.getMessage().contains("429")) {
				rateLimitExceeded.set(true);
			}
		}

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

						if (rateLimitExceeded.get()) {
							try {
								Thread.sleep(pauseDurationMillis);
							} catch (InterruptedException e) {
							}
						}

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
}
