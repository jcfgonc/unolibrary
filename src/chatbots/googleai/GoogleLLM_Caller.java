package chatbots.googleai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.google.gson.Gson;

import chatbots.googleai.json.Candidate;
import chatbots.googleai.json.ChatBotReply;
import chatbots.googleai.json.Content;
import chatbots.googleai.json.Part;
import utils.VariousUtils;

public class GoogleLLM_Caller {
	private static final Gson GSON = new Gson();
	public static String model = "gemini-1.5-flash";
	private static String api_call_template;
	private static String api_key;
	private static String api_url;
	private static boolean initialized = false;
	private static URL url;

	private static void init() throws IOException, URISyntaxException {
		if (initialized)
			return;

		api_call_template = VariousUtils.readFile("data/google_gemini_api_query.json");
		api_key = VariousUtils.readFile("data/apikey.txt");
		api_url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + api_key;
		url = new URI(api_url).toURL();

		initialized = true;
	}

	public static String doRequest(String text) throws IOException, URISyntaxException {
		init();

		String reply = null;
		while (reply == null) {
			reply = doREST_HTTP_Request(text);
			if (reply == null) {
				try {
			//		System.err.println("call failed, retrying in 2s");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else
				return reply;
		}
		return null;
	}

	private static String doREST_HTTP_Request(String text) throws IOException, URISyntaxException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
//			connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
		{
			String requestContents = api_call_template.replace("%text%", text);
			byte[] out = requestContents.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = connection.getOutputStream();
			stream.write(out);
			stream.flush();
		}

		int responseCode = connection.getResponseCode();
//		System.out.println(responseCode + " " + connection.getResponseMessage()); // THis is optional
		if (responseCode == 200) {

			InputStream inputStream = connection.getInputStream();
			String raw_reply = toString(inputStream);
			// System.out.println(raw_reply);
			inputStream.close();
			connection.disconnect();

			// process chatbot reply in json format
			ChatBotReply chatbotReply = GSON.fromJson(raw_reply, ChatBotReply.class);
			ArrayList<Candidate> candidates = chatbotReply.candidates;
			Candidate candidate = candidates.get(0);
			Content content = candidate.content;
			ArrayList<Part> parts = content.parts;
			Part part = parts.get(0);
			String chatbotText = part.text;

			return chatbotText;
		} else {
			return null;
		}
	}

	private static String toString(InputStream input) throws IOException {
		return new String(input.readAllBytes(), StandardCharsets.UTF_8);
	}

}
