package chatbots.googleai.json;

public class UsageMetadata {
	public int promptTokenCount;
	public int candidatesTokenCount;
	public int totalTokenCount;

	@Override
	public String toString() {
		return "promptTokenCount=" + promptTokenCount + ", candidatesTokenCount=" + candidatesTokenCount + ", totalTokenCount=" + totalTokenCount ;
	}
}
