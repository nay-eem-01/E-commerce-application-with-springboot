package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.media.ImageService;
import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "Admin: uploads", description = "Product images")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

	private final ImageService imageService;
	private final CurrentUser currentUser;

	@Operation(summary = "Upload a product image (JPEG, PNG or WebP, at most 5 MB). Stored as WebP; returns {url} for product media.")
	@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Response> upload(@RequestPart("file") MultipartFile file) throws IOException {
		return Response.created("Image uploaded.", Map.of("url", imageService.upload(file.getBytes(), currentUser.id())));
	}
}
