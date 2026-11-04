package com.ecommerce.catalog.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * Images in an S3 bucket (or R2 / MinIO through {@code S3_ENDPOINT}), served from {@code UPLOADS_PUBLIC_BASE_URL}
 * (the CDN or the bucket's public URL). No ACL is set: the bucket policy or the CDN makes objects readable.
 */
@Component
@ConditionalOnExpression("!'${app.storage.s3.bucket:}'.isBlank()")
public class S3ImageStorage implements ImageStorage {

	private final StorageProperties properties;
	private final S3Client s3;

	public S3ImageStorage(StorageProperties properties) {
		this(properties, client(properties.s3()));
	}

	S3ImageStorage(StorageProperties properties, S3Client s3) {
		this.properties = properties;
		this.s3 = s3;
	}

	private static S3Client client(StorageProperties.S3 c) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(c.region()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKeyId(), c.secretAccessKey())));
		if (c.endpoint() != null && !c.endpoint().isBlank()) {
			// MinIO needs path-style addressing; R2 accepts it too.
			builder.endpointOverride(URI.create(c.endpoint())).forcePathStyle(true);
		}
		return builder.build();
	}

	@Override
	public String store(byte[] webp) {
		String name = properties.newFileName();
		s3.putObject(PutObjectRequest.builder()
						.bucket(properties.s3().bucket())
						.key(name)
						.contentType("image/webp")
						// names are never reused, so browsers and the CDN may keep a copy for a year
						.cacheControl("public, max-age=31536000, immutable")
						.build(),
				RequestBody.fromBytes(webp));
		return properties.publicUrl(name);
	}

	@Override
	public boolean isStored(String url) {
		return properties.isPublicUrl(url);
	}
}
