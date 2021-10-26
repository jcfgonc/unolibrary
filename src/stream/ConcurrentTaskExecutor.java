package stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import utils.OSTools;

public class ConcurrentTaskExecutor<T> {

	private ExecutorService executorService;
	private int numberOfThreads;

	public ConcurrentTaskExecutor(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		this.executorService = Executors.newFixedThreadPool(numberOfThreads);
		System.err.println("ConcurrentTaskExecutor running with " + getNumberOfThreads() + " threads");
	}

	public ConcurrentTaskExecutor() {
		this(OSTools.getNumberOfCores());
	}

	public void parallelForEach(Collection<T> col, Consumer<? super T> action) throws InterruptedException {
		ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for (T element : col) {
			tasks.add(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					action.accept(element);
					return null;
				}
			});
		}
		executorService.invokeAll(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public void parallelForEach(T[] array, Consumer<? super T> action) throws InterruptedException {
		ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for (T element : array) {
			tasks.add(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					action.accept(element);
					return null;
				}
			});
		}
		executorService.invokeAll(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public void parallelForEach(int[] array, IntConsumer action) throws InterruptedException {
		ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for (int a : array) {
			tasks.add(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					action.accept(Integer.valueOf(a));
					return null;
				}
			});
		}
		executorService.invokeAll(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public void parallelForEach(int numElements, Consumer<Integer> action) throws InterruptedException {
		ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		for (int i = 0; i < numElements; i++) {
			final int final_i = i;
			tasks.add(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					action.accept(Integer.valueOf(final_i));
					return null;
				}
			});
		}
		executorService.invokeAll(tasks, Long.MAX_VALUE, TimeUnit.DAYS);
	}

	public void shutdown() {
		executorService.shutdown();
	}

	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}

}
