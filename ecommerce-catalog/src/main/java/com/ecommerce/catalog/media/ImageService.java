package com.ecommerce.catalog.media;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

/**
 * Product images: JPEG, PNG or WebP up to 5 MB in, a public WebP URL out. JPEG and PNG are decoded by the JDK's own
 * (pure Java) readers, scaled down to 2 400 px and encoded to WebP; WebP uploads are stored as they are and never
 * decoded here, so untrusted bytes never reach native code. Dimensions are checked from the header before decoding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

	public static final long MAX_BYTES = 5L * 1024 * 1024;
	static final int MAX_SIDE = 2400;
	static final int LIMIT_SIDE = 8000;
	static final long LIMIT_PIXELS = 40_000_000L;
	static final float QUALITY = 0.82f;
	static final int UPLOADS_PER_MINUTE = 60;
	/** A 40 MP decode takes about 120 MB; more at once ran a 512 MB server out of memory. Per instance. */
	static final int CONVERSIONS_AT_ONCE = 2;

	private final ImageStorage storage;
	private final RateLimiter rateLimiter;
	final Semaphore converting = new Semaphore(CONVERSIONS_AT_ONCE, true);

	public String upload(byte[] bytes, long userId) {
		// bounds bucket spend if a staff token leaks
		rateLimiter.hit("upload:" + userId, UPLOADS_PER_MINUTE, Duration.ofMinutes(1));
		if (bytes == null || bytes.length == 0) throw ResponseException.badRequest(ErrorCode.IMAGE_REQUIRED);
		if (bytes.length > MAX_BYTES) throw ResponseException.badRequest(ErrorCode.IMAGE_TOO_LARGE);
		ImageType type = ImageType.detect(bytes).orElseThrow(() -> ResponseException.badRequest(ErrorCode.IMAGE_TYPE_NOT_ALLOWED));
		byte[] webp;
		if (type == ImageType.WEBP) {
			int[] size = ImageType.webpSize(bytes).orElseThrow(() -> ResponseException.badRequest(ErrorCode.IMAGE_UNREADABLE));
			checkSize(size[0], size[1]);
			webp = bytes;
		} else {
			converting.acquireUninterruptibly();
			try {
				webp = toWebp(bytes, type);
			} finally {
				converting.release();
			}
		}
		String url = storage.store(webp);
		log.info("Image uploaded by user {}: {} bytes {} -> {} bytes webp", userId, bytes.length, type, webp.length);
		return url;
	}

	/** Decodes a JPEG or PNG (header checked first), scales it to at most 2 400 px and encodes lossy WebP. */
	byte[] toWebp(byte[] bytes, ImageType type) {
		BufferedImage source;
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) throw ResponseException.badRequest(ErrorCode.IMAGE_UNREADABLE);
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, true, true);
				checkSize(reader.getWidth(0), reader.getHeight(0));
				source = reader.read(0);
			} finally {
				reader.dispose();
			}
		} catch (IOException | RuntimeException e) {
			if (e instanceof ResponseException re) throw re;
			// CMYK JPEGs, truncated files and other images the JDK cannot read
			throw ResponseException.badRequest(ErrorCode.IMAGE_UNREADABLE);
		}
		return encode(scaled(source, type == ImageType.PNG && source.getColorModel().hasAlpha()));
	}

	private static void checkSize(int width, int height) {
		if (width <= 0 || height <= 0) throw ResponseException.badRequest(ErrorCode.IMAGE_UNREADABLE);
		if (width > LIMIT_SIDE || height > LIMIT_SIDE || (long) width * height > LIMIT_PIXELS) {
			throw ResponseException.badRequest(ErrorCode.IMAGE_TOO_MANY_PIXELS);
		}
	}

	/** Redraws into a plain RGB(A) image (what the encoder takes), scaled down so the longest side is at most 2 400 px. */
	private static BufferedImage scaled(BufferedImage src, boolean alpha) {
		double factor = Math.min(1.0, (double) MAX_SIDE / Math.max(src.getWidth(), src.getHeight()));
		int w = Math.max(1, (int) Math.round(src.getWidth() * factor));
		int h = Math.max(1, (int) Math.round(src.getHeight() * factor));
		BufferedImage out = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return out;
	}

	private static byte[] encode(BufferedImage image) {
		ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionType("Lossy");
		param.setCompressionQuality(QUALITY);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), param);
		} catch (IOException e) {
			throw new IllegalStateException("WebP encoding failed", e);
		} finally {
			writer.dispose();
		}
		return out.toByteArray();
	}
}
