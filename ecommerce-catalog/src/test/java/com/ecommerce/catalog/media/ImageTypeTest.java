package com.ecommerce.catalog.media;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

	@Test
	void detectsByFirstBytesOnly() {
		assertThat(ImageType.detect(Images.encode(4, 4, "jpg"))).contains(ImageType.JPEG);
		assertThat(ImageType.detect(Images.encode(4, 4, "png"))).contains(ImageType.PNG);
		assertThat(ImageType.detect(Images.webp("VP8L", new byte[]{0x2f, 0, 0, 0, 0}))).contains(ImageType.WEBP);
	}

	@Test
	void rejectsEverythingElse() {
		assertThat(ImageType.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes())).isEmpty();
		assertThat(ImageType.detect(new byte[0])).isEmpty();
		assertThat(ImageType.detect(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
		assertThat(ImageType.detect(null)).isEmpty();
		assertThat(ImageType.detect("RIFF0000WAVE".getBytes())).isEmpty();
	}

	@Test
	void readsWebpSizeFromEachHeaderKind() {
		// VP8 (lossy): frame tag (3), start code 9d 01 2a, then 14-bit width and height
		byte[] vp8 = {0, 0, 0, (byte) 0x9d, 0x01, 0x2a, (byte) 0x20, 0x03, (byte) 0x58, 0x02};
		assertThat(ImageType.webpSize(Images.webp("VP8 ", vp8))).hasValueSatisfying(s -> assertThat(s).containsExactly(800, 600));

		// VP8L (lossless): signature 0x2f, then 14 bits width-1 and 14 bits height-1, little endian
		int w = 1200 - 1, h = 900 - 1;
		long bits = w | ((long) h << 14);
		byte[] vp8l = {0x2f, (byte) bits, (byte) (bits >> 8), (byte) (bits >> 16), (byte) (bits >> 24)};
		assertThat(ImageType.webpSize(Images.webp("VP8L", vp8l))).hasValueSatisfying(s -> assertThat(s).containsExactly(1200, 900));

		// VP8X (extended): flags (4), then 24-bit width-1 and height-1
		byte[] vp8x = new byte[10];
		int cw = 9000 - 1, ch = 4500 - 1;
		vp8x[4] = (byte) cw; vp8x[5] = (byte) (cw >> 8); vp8x[6] = (byte) (cw >> 16);
		vp8x[7] = (byte) ch; vp8x[8] = (byte) (ch >> 8); vp8x[9] = (byte) (ch >> 16);
		assertThat(ImageType.webpSize(Images.webp("VP8X", vp8x))).hasValueSatisfying(s -> assertThat(s).containsExactly(9000, 4500));
	}

	@Test
	void malformedWebpHasNoSize() {
		assertThat(ImageType.webpSize(Images.webp("VP8 ", new byte[]{0, 0, 0, 1, 2, 3}))).isEmpty(); // bad start code
		assertThat(ImageType.webpSize(Arrays.copyOf(Images.webp("VP8X", new byte[10]), 20))).isEmpty(); // cut short
		assertThat(ImageType.webpSize(Images.webp("ALPH", new byte[10]))).isEmpty();
	}
}
