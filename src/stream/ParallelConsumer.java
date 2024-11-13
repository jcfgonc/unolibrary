package stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import utils.OSTools;

public class ParallelConsumer<T> {

	private StreamService ss;

	public ParallelConsumer(int numberOfThreads) {
		this.ss = new StreamService(numberOfThreads);
		System.err.println("Parallel Consumer running with " + getNumberOfThreads() + " threads");
	}

	public ParallelConsumer() {
		this(OSTools.getNumberOfCores());
	}

	public void parallelForEach(Collection<T> col, Consumer<? super T> action) throws InterruptedException {
		ArrayList<T> list;
		if (col instanceof ArrayList<?>) {
			list = (ArrayList<T>) col;
		} else {
			list = new ArrayList<>(col);
		}
		StreamProcessor sp = new StreamProcessor() {

			@Override
			public void run(int processorId, int rangeL, int rangeH, int streamSize) {
				for (int i = rangeL; i <= rangeH; i++) {
					T element = list.get(i);
					action.accept(element);
				}
			}
		};
		ss.invoke(list.size(), sp);
	}

	public void parallelForEach(T[] array, Consumer<? super T> action) throws InterruptedException {
		StreamProcessor sp = new StreamProcessor() {

			@Override
			public void run(int processorId, int rangeL, int rangeH, int streamSize) {
				for (int i = rangeL; i <= rangeH; i++) {
					T element = array[i];
					action.accept(element);
				}
			}
		};
		ss.invoke(array.length, sp);
	}

	public void parallelForEach(int[] array, IntConsumer action) throws InterruptedException {
		StreamProcessor sp = new StreamProcessor() {

			@Override
			public void run(int processorId, int rangeL, int rangeH, int streamSize) {
				for (int i = rangeL; i <= rangeH; i++) {
					int element = array[i];
					action.accept(element);
				}
			}
		};
		ss.invoke(array.length, sp);
	}

	public void parallelForEach(int numElements, Consumer<Integer> action) throws InterruptedException {
		StreamProcessor sp = new StreamProcessor() {

			@Override
			public void run(int processorId, int rangeL, int rangeH, int streamSize) {
				for (int i = rangeL; i <= rangeH; i++) {
					action.accept(i);
				}
			}
		};
		ss.invoke(numElements, sp);
	}

	public void shutdown() {
		ss.shutdown();
	}

	/**
	 * DEMO
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			list.add(i);
		}

		// --- DEMO
		ParallelConsumer<Integer> pc = new ParallelConsumer<>(4);
		pc.parallelForEach(list, element -> {
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(element);
		});
		System.out.println("waiting");
		pc.shutdown();
		System.out.println("shutdown");
	}

	public int getNumberOfThreads() {
		return ss.getNumberOfThreads();
	}

}
