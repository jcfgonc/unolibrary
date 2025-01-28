package chatbots.googleai;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

public class OpenAiLLM_Caller {

	public static void main(String[] args) throws IOException, URISyntaxException {
		doRequest("");
	}

	public static String llm_model = "gpt-4-turbo";
	private static String api_key;
	private static boolean initialized = false;

	private static void init() throws IOException, URISyntaxException {
		if (initialized)
			return;
		api_key = "";
		// VariousUtils.readFile("data/apikey.txt");

		initialized = true;
	}

	public static String doRequest(String text) throws IOException, URISyntaxException {
		init();

		SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(api_key).build();

		String sysmessage = "You are an expert in AI.";
		String prompt = "You are a knowledge base that answers to questions made by an expert system. All your knowledge is in american english. You have a comprehensive ontology and knowledge base that spans the basic concepts and rules about how the world works. You do not explain your answer nor your reasoning. You give all possible answers. Try to be as specific as possible and do not generalize. What are the parts and their purpose of a church? give each purpose as a single action verb";
		ChatRequest chatRequest = ChatRequest.builder().model(llm_model).message(SystemMessage.of(sysmessage))
				.message(UserMessage.of(prompt)).temperature(0.0).maxCompletionTokens(300).build();
		CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
		Chat chatResponse = futureChat.join();
		System.out.println(chatResponse.firstContent());
		return text;
	}

	public static boolean askLLM_If_child_isa_parent(String child, String parent) throws IOException, URISyntaxException {
		String prompt = "Use american english. Answer yes or no. If there are typos or spelling errors in the question, answer no. This is the question: is %s a %s?";
		String text = String.format(prompt, child, parent);
		System.out.println(text);
		String reply = doRequest(text).toLowerCase().trim();
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

}
