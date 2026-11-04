package com.ecommerce.catalog.media;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Test images drawn in memory. */
final class Images {
	private Images() {}

	static byte[] encode(int width, int height, String format) {
		BufferedImage img = new BufferedImage(width, height, "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setPaint(new GradientPaint(0, 0, Color.PINK, width, height, Color.BLUE));
		g.fillRect(0, 0, width, height);
		g.dispose();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, format, out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	static BufferedImage decode(byte[] bytes) {
		try {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** A RIFF/WEBP container holding one chunk of the given type and payload. */
	static byte[] webp(String chunk, byte[] payload) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes("RIFF".getBytes());
		out.writeBytes(le32(4 + 8 + payload.length));
		out.writeBytes("WEBP".getBytes());
		out.writeBytes(chunk.getBytes());
		out.writeBytes(le32(payload.length));
		out.writeBytes(payload);
		return out.toByteArray();
	}

	static byte[] le32(int v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
	}
}
