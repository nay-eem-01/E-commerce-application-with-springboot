package com.ecommerce.catalog.media;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceTest {

	private final ImageStorage storage = mock(ImageStorage.class);
	private final ImageService service = new ImageService(storage, new InMemoryRateLimiter());

	{
		when(storage.store(any())).thenReturn("https://cdn.senpai.shop/x.webp");
	}

	private byte[] stored() {
		ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
		verify(storage).store(bytes.capture());
		return bytes.getValue();
	}

	private void assertRejected(byte[] bytes, String message) {
		assertThatThrownBy(() -> service.upload(bytes, 1L))
				.isInstanceOfSatisfying(ResponseException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
				.hasMessage(message);
		verify(storage, never()).store(any());
	}

	@Test
	void emptyIs400() {
		assertRejected(new byte[0], ErrorCode.IMAGE_REQUIRED);
	}

	@Test
	void over5MbIs400() {
		byte[] big = Arrays.copyOf(Images.encode(10, 10, "png"), (int) ImageService.MAX_BYTES + 1);
		assertRejected(big, ErrorCode.IMAGE_TOO_LARGE);
	}

	@Test
	void textFileIs400() {
		assertRejected("hello".getBytes(), ErrorCode.IMAGE_TYPE_NOT_ALLOWED);
	}

	@Test
	void pngBecomesWebpAtMost2400() {
		assertThat(service.upload(Images.encode(3000, 1500, "png"), 1L)).isEqualTo("https://cdn.senpai.shop/x.webp");
		byte[] webp = stored();
		assertThat(ImageType.detect(webp)).contains(ImageType.WEBP);
		BufferedImage back = Images.decode(webp);
		assertThat(back.getWidth()).isEqualTo(2400);
		assertThat(back.getHeight()).isEqualTo(1200);
	}

	@Test
	void smallJpegIsNotScaledUp() {
		service.upload(Images.encode(800, 600, "jpg"), 1L);
		BufferedImage back = Images.decode(stored());
		assertThat(back.getWidth()).isEqualTo(800);
		assertThat(back.getHeight()).isEqualTo(600);
	}

	@Test
	void tooManyPixelsIs400WithoutDecoding() {
		// a valid PNG signature and IHDR claiming 9000 x 9000, with no pixel data at all
		byte[] png = Images.encode(1, 1, "png");
		byte[] header = Arrays.copyOf(png, 33);
		header[16] = 0; header[17] = 0; header[18] = 0x23; header[19] = 0x28; // width 9000
		header[20] = 0; header[21] = 0; header[22] = 0x23; header[23] = 0x28; // height 9000
		assertRejected(header, ErrorCode.IMAGE_TOO_MANY_PIXELS);
	}

	@Test
	void tooManyPixelsInAWebpHeaderIs400() {
		byte[] vp8x = new byte[10];
		int side = 9000 - 1;
		vp8x[4] = (byte) side; vp8x[5] = (byte) (side >> 8);
		vp8x[7] = (byte) side; vp8x[8] = (byte) (side >> 8);
		assertRejected(Images.webp("VP8X", vp8x), ErrorCode.IMAGE_TOO_MANY_PIXELS);
	}

	@Test
	void webpIsStoredUnchanged() {
		byte[] vp8 = {0, 0, 0, (byte) 0x9d, 0x01, 0x2a, (byte) 0x20, 0x03, (byte) 0x58, 0x02};
		byte[] webp = Images.webp("VP8 ", vp8);
		service.upload(webp, 1L);
		assertThat(stored()).isEqualTo(webp);
	}

	@Test
	void unreadableImageIs400() {
		byte[] png = Images.encode(400, 300, "png");
		assertRejected(Arrays.copyOf(png, 60), ErrorCode.IMAGE_UNREADABLE); // valid header, pixels cut off
	}

	@Test
	void sixtyFirstUploadInAMinuteIs429() {
		byte[] small = Images.encode(8, 8, "jpg");
		for (int i = 0; i < ImageService.UPLOADS_PER_MINUTE; i++) service.upload(small, 7L);
		assertThatThrownBy(() -> service.upload(small, 7L))
				.isInstanceOfSatisfying(ResponseException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
		service.upload(small, 8L); // another staff member is not affected
	}

	/**
	 * Regression: four 40-megapixel uploads at once ran a 512 MB server out of memory (each decode is about 120 MB).
	 * Conversions now take a permit: at most CONVERSIONS_AT_ONCE run, the rest wait.
	 */
	@Test
	void conversionsWaitForAPermit() throws Exception {
		service.converting.acquire(ImageService.CONVERSIONS_AT_ONCE);
		java.util.concurrent.CompletableFuture<String> upload =
				java.util.concurrent.CompletableFuture.supplyAsync(() -> service.upload(Images.encode(40, 30, "png"), 1L));
		Thread.sleep(300);
		assertThat(upload).isNotDone();
		service.converting.release(ImageService.CONVERSIONS_AT_ONCE);
		assertThat(upload.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("https://cdn.senpai.shop/x.webp");
	}
}
