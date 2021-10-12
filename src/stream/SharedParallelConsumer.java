package stream;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Statically allocated Parallel Consumer - easily shared by multiple classes.
 */
public class SharedParallelConsumer<T> {
	private static ParallelConsumer parallelConsumer;
	private static boolean initialized = false;

	private static <T> void initialize() {
		if (initialized)
			return;
		parallelConsumer = new ParallelConsumer<T>();
		initialized = true;
	}

	public static <T> void parallelForEach(Collection<T> col, Consumer<? super T> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(col, action);
	}

	public static <T> void parallelForEach(T[] array, Consumer<? super T> action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(array, action);
	}

	public static <T> void parallelForEach(int[] array, IntConsumer action) throws InterruptedException {
		initialize();
		parallelConsumer.parallelForEach(array, action);
	}

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
