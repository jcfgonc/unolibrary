package stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import utils.OSTools;

public class ParallelConsumer<T> {

	private StreamService ss;

	public ParallelConsumer(int numberOfThreads) {
		this.ss = new StreamService(numberOfThreads);
	}

	public ParallelConsumer() {
		int numberOfThreads = OSTools.getNumberOfCores();
		System.out.printf("using %d threads for the objective evaluation\n", numberOfThreads);
		this.ss = new StreamService(numberOfThreads);
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

	public void shutdown() {
		ss.shutdown();
	}

	/**
	 * DEMO
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void test(String[] args) throws InterruptedException {
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			list.add(i);
		}

		// --- DEMO
		ParallelConsumer<Integer> pc = new ParallelConsumer<>(4);
		pc.parallelForEach(list, element -> {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(element);
		});
		System.out.println("waiting");
		pc.shutdown();
		System.out.println("shutdown");
	}

}
