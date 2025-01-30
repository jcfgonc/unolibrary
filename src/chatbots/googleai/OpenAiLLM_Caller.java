package chatbots.googleai;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

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
import utils.VariousUtils;

public class OpenAiLLM_Caller {

	public static final String llm_model = "gpt-4o-mini";
	private static String api_key;
	private static boolean initialized = false;
	private static SimpleOpenAI openAI;
	private static ChatCompletions chatCompletions;

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

		ChatRequest chatRequest = ChatRequest.builder().model(llm_model).message(UserMessage.of(prompt)).temperature(0.0).maxCompletionTokens(1024).build();
		CompletableFuture<Chat> futureChat = chatCompletions.create(chatRequest);
		Chat chatResponse = futureChat.join();
		String firstContent = chatResponse.firstContent();
//		System.out.println(firstContent);
		return firstContent;
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
		// System.out.println(text);
		String reply = doRequest(text);

		reply = reply.toLowerCase().strip();
		System.out.println(reply);
		reply = reply.replace("\\", "/"); // you never know...
		reply = reply.replace("/", " ");
		reply = reply.replace("\t", " "); // tabs -> spaces
		reply = reply.replaceAll(" [ ]+", " "); // multiple spaces -> one space
		reply = reply.replace("\r\n", "\n"); // windows -> unix newline
		reply = reply.replaceAll("[\n]+", "\n"); // empty lines
		reply = reply.replace(" \n", "\n"); // empty lines

		BooleanArrayList facts_evaluations = new BooleanArrayList();
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
				System.err.printf("unrecognized answer|%s|\n", valid);
			}
		}
//		assert facts_evaluations.size() == edges.size();
		return facts_evaluations;
	}

	public static void checkISA(StringGraph graph) throws IOException, URISyntaxException {
		StringGraph trueFacts = new StringGraph();
		StringGraph falseFacts = new StringGraph();
		final int batchSize = 20;
		int counter = 0;
		final int queryLimit = 1500;
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
						trueFacts.addEdge(edge);
					} else {
						falseFacts.addEdge(edge);
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
		graph.removeEdges(trueFacts.edgeSet());
		graph.removeEdges(falseFacts.edgeSet());
	}

}
