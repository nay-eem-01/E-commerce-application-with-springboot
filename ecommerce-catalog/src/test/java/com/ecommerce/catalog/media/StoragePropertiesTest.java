package com.ecommerce.catalog.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePropertiesTest {

	private static final String UUID_NAME = "3f2b8c1e-5a7d-4e2f-9b1c-0d6e8a4f2b7c.webp";
	private final StorageProperties local = new StorageProperties("uploads", "https://cdn.senpai.shop/img/", StorageProperties.S3.NONE);

	@Test
	void recognisesOnlyItsOwnWebpUrls() {
		assertThat(local.isPublicUrl("https://cdn.senpai.shop/img/" + UUID_NAME)).isTrue();
		assertThat(local.isPublicUrl(local.publicUrl(local.newFileName()))).isTrue();
		assertThat(local.isPublicUrl("https://evil.example/img/" + UUID_NAME)).isFalse();
		assertThat(local.isPublicUrl("https://cdn.senpai.shop/img/../" + UUID_NAME)).isFalse();
		assertThat(local.isPublicUrl("https://cdn.senpai.shop/img/3f2b8c1e-5a7d-4e2f-9b1c-0d6e8a4f2b7c.png")).isFalse();
		assertThat(local.isPublicUrl("https://cdn.senpai.shop/img/" + UUID_NAME + "?x=1")).isFalse();
		assertThat(local.isPublicUrl("https://cdn.senpai.shop/img/sub/" + UUID_NAME)).isFalse();
		assertThat(local.isPublicUrl(null)).isFalse();
	}

	@Test
	void newNamesAreRandomWebp() {
		assertThat(local.newFileName()).endsWith(".webp").isNotEqualTo(local.newFileName());
	}

	@Test
	void aBucketNeedsRegionKeysAndAPublicUrl() {
		assertThatThrownBy(() -> new StorageProperties("uploads", "https://cdn.senpai.shop",
				new StorageProperties.S3("senpai-img", "", null, "key", "secret"))).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new StorageProperties("uploads", "https://cdn.senpai.shop",
				new StorageProperties.S3("senpai-img", "ap-southeast-1", null, "key", ""))).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new StorageProperties("uploads", "",
				new StorageProperties.S3("senpai-img", "ap-southeast-1", null, "key", "secret"))).isInstanceOf(IllegalStateException.class);
		assertThat(new StorageProperties("uploads", "https://cdn.senpai.shop",
				new StorageProperties.S3("senpai-img", "ap-southeast-1", null, "key", "secret")).s3().enabled()).isTrue();
	}
}
