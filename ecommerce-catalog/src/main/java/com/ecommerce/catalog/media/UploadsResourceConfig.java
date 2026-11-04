package com.ecommerce.catalog.media;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves local-disk uploads read-only at /uploads/** (development; production uses S3 and a CDN). */
@Configuration
@ConditionalOnExpression("'${app.storage.s3.bucket:}'.isBlank()")
@RequiredArgsConstructor
public class UploadsResourceConfig implements WebMvcConfigurer {

	private final StorageProperties properties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(Path.of(properties.localDir()).toAbsolutePath().toUri() + "/");
	}
}
