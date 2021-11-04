package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Custom version of BufferedReader which maintains the buffer full in background. Adapted from https://stackoverflow.com/a/32013595
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class NonblockingBufferedReader {
	private static final int BUFSIZE = 1 << 24;
	final BlockingQueue<String> lines = new ArrayBlockingQueue<String>(1 << 24);
	volatile boolean closed = false;
	Thread backgroundReaderThread = null;
	BufferedReader br;

	public NonblockingBufferedReader(String filename) throws IOException {
		this(new FileReader(filename, StandardCharsets.UTF_8));
	}

	public NonblockingBufferedReader(Reader in) throws IOException {
		br = new BufferedReader(in, BUFSIZE);
		startBackgroundThread();
	}

	public NonblockingBufferedReader(InputStream input) throws UnsupportedEncodingException {
		br = new BufferedReader(new InputStreamReader(input, "UTF-8"), BUFSIZE);
		startBackgroundThread();
	}

	private void startBackgroundThread() {
		backgroundReaderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!Thread.interrupted()) {
						String line = br.readLine();
						if (line == null) {
							break;
						}
						if (line.isBlank()) // empty lines are useless
							continue;
						lines.offer(line, Long.MAX_VALUE, TimeUnit.DAYS);
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} finally {
					try {
						br.close();
					} catch (IOException e) {
					}
					closed = true;
					backgroundReaderThread = null;
				}
			}
		});
		backgroundReaderThread.setDaemon(true);
		backgroundReaderThread.start();
	}

	public String readLine() throws IOException {
		try {
			if (closed && lines.isEmpty())
				return null;
			return lines.poll(500L, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("The BackgroundReaderThread was interrupted!", e);
		}
	}

	public void close() throws IOException {
		if (backgroundReaderThread != null) {
			backgroundReaderThread.interrupt();
			backgroundReaderThread = null;
			br.close();
		}
	}
}