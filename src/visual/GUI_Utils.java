package visual;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class GUI_Utils {
	private static ExecutorService savingExecutors = Executors.newFixedThreadPool(1);

	public static void saveScreenShotPNG(BufferedImage img, String filename) {
		CountDownLatch latch = new CountDownLatch(1); // used to block calling thread
		savingExecutors.execute(() -> {
			try {
				// ImageWriter is single-threaded, can not be shared between multiple threads
				// generates Exception in thread "pool-3-thread-3" java.lang.IndexOutOfBoundsException: pos < flushedPos!
				ImageWriter pngWriter = ImageIO.getImageWritersByFormatName("png").next();
				ImageWriteParam parameters = pngWriter.getDefaultWriteParam();
				// set blasters to full
				if (parameters.canWriteCompressed()) {
					parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					parameters.setCompressionQuality(0.5f);
				}

				ImageOutputStream out = ImageIO.createImageOutputStream(new File(filename));
				pngWriter.setOutput(out);
				pngWriter.write(null, new IIOImage(img, null, null), parameters);
				out.close();

				latch.countDown(); // notify calling thread
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		try {
			// wait on called thread
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static RenderingHints createDefaultRenderingHints() {
		RenderingHints renderingHints = new RenderingHints(null);
		renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		renderingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		renderingHints.put(RenderingHints.KEY_RESOLUTION_VARIANT, RenderingHints.VALUE_RESOLUTION_VARIANT_DPI_FIT);
		renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return renderingHints;
	}

}
