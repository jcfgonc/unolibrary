package stream;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Statically allocated Parallel Consumer - easily shared by multiple classes.
 */
public class SharedParallelConsumer<T> {
	private static ConcurrentTaskExecutor parallelConsumer;
	private static boolean initialized = false;

	private static <T> void initialize() {
		if (initialized)
			return;
		parallelConsumer = new ConcurrentTaskExecutor<T>();
		initialized = true;
	}

	// called by MOEA's evaluateAll()
	public static <T> void parallelForEach(Collection<T> col, Consumer<? super T> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(col, action);
	}

	// currently not used
	public static <T> void parallelForEach(T[] array, Consumer<? super T> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(array, action);
	}

	// called by MOEA's RandomInitialization.initialize()
	public static <T> void parallelForEach(int[] array, IntConsumer action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(array, action);
	}

	// called by MOEA's NSGAII.iterate()
	public static <T> void parallelForEach(int numElements, Consumer<Integer> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(numElements, action);
	}

	public static <T> void shutdown() {
		if (initialized) {
			parallelConsumer.shutdown();
			initialized = false;
		}
	}
}
