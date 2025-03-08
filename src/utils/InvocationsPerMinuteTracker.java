package utils;

public class InvocationsPerMinuteTracker {

	// Holds the identifier for the current minute window.
	private static double startTime = System.currentTimeMillis();

	// Count of invocations during the current minute.
	private static int currentCount = 0;

	public static synchronized int printInvocationsPerMinute(String text) {
		double currentTime = System.currentTimeMillis();
		if (currentTime >= startTime + 60000) {
			// if one minute has changed reset the counter and update the minute
			System.err.printf("%s\tInvocations Per Minute\t%d\n", text, currentCount);
			startTime = currentTime;
			currentCount = 0;
		}
		// Increase the count for every invocation in the current minute.
		currentCount++;
		return currentCount;
	}
}
