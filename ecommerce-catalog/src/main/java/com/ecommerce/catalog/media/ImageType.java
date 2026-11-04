package com.ecommerce.catalog.media;

import java.util.Optional;

/** Allowed upload formats, recognised by their first bytes, never by the file name or the browser's content type. */
public enum ImageType {
	JPEG, PNG, WEBP;

	private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	public static Optional<ImageType> detect(byte[] bytes) {
		if (bytes == null) return Optional.empty();
		if (startsWith(bytes, 0, JPEG_SIGNATURE)) return Optional.of(JPEG);
		if (startsWith(bytes, 0, PNG_SIGNATURE)) return Optional.of(PNG);
		// WebP: "RIFF" <4-byte size> "WEBP"
		if (bytes.length >= 12 && startsWith(bytes, 0, "RIFF".getBytes()) && startsWith(bytes, 8, "WEBP".getBytes())) {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	/**
	 * Width and height of a WebP read from its first chunk header, without decoding it (WebP is never decoded on the
	 * server). Empty when the header is malformed or of a kind we do not read.
	 */
	public static Optional<int[]> webpSize(byte[] b) {
		if (detect(b).orElse(null) != WEBP || b.length < 20) return Optional.empty();
		String chunk = new String(b, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
		int p = 20; // first chunk's payload
		int need = "VP8L".equals(chunk) ? 5 : 10; // header bytes each kind needs
		if (b.length < p + need) return Optional.empty();
		return switch (chunk) {
			case "VP8 " -> (b[p + 3] & 0xFF) == 0x9d && (b[p + 4] & 0xFF) == 0x01 && (b[p + 5] & 0xFF) == 0x2a
					? Optional.of(new int[]{u16(b, p + 6) & 0x3FFF, u16(b, p + 8) & 0x3FFF})
					: Optional.empty();
			case "VP8L" -> {
				if ((b[p] & 0xFF) != 0x2f) yield Optional.empty();
				long bits = (b[p + 1] & 0xFFL) | (b[p + 2] & 0xFFL) << 8 | (b[p + 3] & 0xFFL) << 16 | (b[p + 4] & 0xFFL) << 24;
				yield Optional.of(new int[]{(int) (bits & 0x3FFF) + 1, (int) ((bits >> 14) & 0x3FFF) + 1});
			}
			case "VP8X" -> Optional.of(new int[]{u24(b, p + 4) + 1, u24(b, p + 7) + 1});
			default -> Optional.empty();
		};
	}

	private static int u16(byte[] b, int i) {
		return (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8;
	}

	private static int u24(byte[] b, int i) {
		return u16(b, i) | (b[i + 2] & 0xFF) << 16;
	}

	private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
		if (bytes.length < offset + prefix.length) return false;
		for (int i = 0; i < prefix.length; i++) {
			if (bytes[offset + i] != prefix[i]) return false;
		}
		return true;
	}
}
