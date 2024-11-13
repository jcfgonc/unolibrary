package stream;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Statically allocated Parallel Consumer - easily shared by multiple classes.
 */
public class SharedParallelConsumer<T> {
	@SuppressWarnings("rawtypes")
	private static ConcurrentTaskExecutor parallelConsumer;
	private static boolean initialized = false;

	public static <T> void initialize() {
		if (initialized)
			return;
		parallelConsumer = new ConcurrentTaskExecutor<T>();
		initialized = true;
	}

	public static <T> void initialize(int numThreads) {
		if (initialized)
			return;
		parallelConsumer = new ConcurrentTaskExecutor<T>(numThreads);
		initialized = true;
	}

	// called by MOEA's evaluateAll()
	@SuppressWarnings("unchecked")
	public static <T> void parallelForEach(Collection<T> col, Consumer<? super T> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(col, action);
	}

	// currently not used
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
