package stream;

public interface StreamProcessor {

	/**
	 * Kind of a OpenMP for(int i=rangeL; i<=rangeH; i++) {}
	 * 
	 * @param processorId
	 *            the stream processor ID
	 * @param rangeL
	 *            lowest index given to this stream processor (inclusive)
	 * @param rangeH
	 *            highest index given to this stream processor (inclusive)
	 * @param sharedInputObject
	 *            object shared between all stream processors (containing stream input)
	 * @param sharedOutputObject
	 *            object shared between all stream processors (containing stream output)
	 * @param streamSize
	 *            size of the stream
	 */
	public void run(int processorId, int rangeL, int rangeH, int streamSize);

}
