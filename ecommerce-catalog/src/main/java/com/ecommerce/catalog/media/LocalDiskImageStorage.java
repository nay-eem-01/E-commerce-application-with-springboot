package com.ecommerce.catalog.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Development only: images under {@code STORAGE_DIR}, served at {@code /uploads/**}. Refuses to start in production. */
@Component
@ConditionalOnExpression("'${app.storage.s3.bucket:}'.isBlank()")
public class LocalDiskImageStorage implements ImageStorage {

	private final StorageProperties properties;

	public LocalDiskImageStorage(StorageProperties properties, Environment environment) {
		if (environment.acceptsProfiles(Profiles.of("production"))) {
			throw new IllegalStateException("Set S3_BUCKET in production: images on the local disk are lost on redeploy");
		}
		this.properties = properties;
	}

	@Override
	public String store(byte[] webp) {
		String name = properties.newFileName();
		try {
			Path dir = Path.of(properties.localDir()).toAbsolutePath();
			Files.createDirectories(dir);
			Files.write(dir.resolve(name), webp);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not store image", e);
		}
		return properties.publicUrl(name);
	}

	@Override
	public boolean isStored(String url) {
		return properties.isPublicUrl(url);
	}
}
