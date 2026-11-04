package com.ecommerce.catalog.media;

/** Where uploaded images live: S3 (or an S3-compatible store) in production, the local disk in development. */
public interface ImageStorage {

	/** Stores WebP bytes under a new random name and returns their public URL. */
	String store(byte[] webp);

	/** True when {@code url} is one {@link #store} returned, so products never point at images on other hosts. */
	boolean isStored(String url);
}
