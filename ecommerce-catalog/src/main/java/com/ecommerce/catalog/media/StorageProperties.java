package com.ecommerce.catalog.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Where uploaded images go. A blank {@code s3.bucket} keeps them on the local disk (development); with a bucket the
 * region, both keys and the public base URL are required, or startup fails.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String localDir, String publicBaseUrl, S3 s3) {

	private static final Pattern NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp");

	/** {@code endpoint} is set only for an S3-compatible store (Cloudflare R2, MinIO). */
	public record S3(String bucket, String region, String endpoint, String accessKeyId, String secretAccessKey) {
		public static final S3 NONE = new S3(null, null, null, null, null);

		public boolean enabled() {
			return bucket != null && !bucket.isBlank();
		}
	}

	public StorageProperties {
		localDir = localDir == null || localDir.isBlank() ? "uploads" : localDir;
		s3 = s3 == null ? S3.NONE : s3;
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			throw new IllegalStateException("app.storage.public-base-url (UPLOADS_PUBLIC_BASE_URL) is required");
		}
		publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
		if (s3.enabled() && (blank(s3.region()) || blank(s3.accessKeyId()) || blank(s3.secretAccessKey()))) {
			throw new IllegalStateException("S3_BUCKET is set: S3_REGION, S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY are required");
		}
	}

	/** Generated, never taken from the upload, so no path tricks are possible. */
	public String newFileName() {
		return UUID.randomUUID() + ".webp";
	}

	public String publicUrl(String fileName) {
		return publicBaseUrl + "/" + fileName;
	}

	/** True only for a URL {@link #publicUrl} could have returned. */
	public boolean isPublicUrl(String url) {
		String prefix = publicBaseUrl + "/";
		return url != null && url.startsWith(prefix) && NAME.matcher(url.substring(prefix.length())).matches();
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}
}
