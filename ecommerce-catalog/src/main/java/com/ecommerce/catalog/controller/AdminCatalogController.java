package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.request.CategoryRequest;
import com.ecommerce.catalog.dto.request.ProductRequest;
import com.ecommerce.catalog.dto.request.StockUpdateRequest;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.service.CategoryService;
import com.ecommerce.catalog.service.ProductAdminService;
import com.ecommerce.catalog.service.ProductQueryService;
import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: catalog")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

	private final CategoryService categoryService;
	private final com.ecommerce.catalog.service.CharacterService characterService;
	private final ProductAdminService productAdminService;
	private final ProductQueryService productQueryService;

	@Operation(summary = "All categories, including inactive ones.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@GetMapping("/categories")
	public ResponseEntity<Response> categories() {
		return Response.ok("Categories loaded.", categoryService.listAll());
	}

	@Operation(summary = "Create a category.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@PostMapping("/categories")
	public ResponseEntity<Response> createCategory(@Valid @RequestBody CategoryRequest request) {
		return Response.created("Category created.", categoryService.create(request));
	}

	@Operation(summary = "Edit a category.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@PutMapping("/categories/{id}")
	public ResponseEntity<Response> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return Response.ok("Category updated.", categoryService.update(id, request));
	}

	@Operation(summary = "Delete an empty category.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@DeleteMapping("/categories/{id}")
	public ResponseEntity<Response> deleteCategory(@PathVariable Long id) {
		categoryService.delete(id);
		return Response.ok("Category deleted.");
	}

	@Operation(summary = "All characters, including inactive ones.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@GetMapping("/characters")
	public ResponseEntity<Response> characters() {
		return Response.ok("Characters loaded.", characterService.listAll());
	}

	@Operation(summary = "Create a character.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@PostMapping("/characters")
	public ResponseEntity<Response> createCharacter(@Valid @RequestBody com.ecommerce.catalog.dto.request.CharacterRequest request) {
		return Response.created("Character created.", characterService.create(request));
	}

	@Operation(summary = "Edit a character.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@PutMapping("/characters/{id}")
	public ResponseEntity<Response> updateCharacter(@PathVariable Long id, @Valid @RequestBody com.ecommerce.catalog.dto.request.CharacterRequest request) {
		return Response.ok("Character updated.", characterService.update(id, request));
	}

	@Operation(summary = "Delete a character that has no products.")
	@PreAuthorize("hasAuthority('CATEGORY_WRITE')")
	@DeleteMapping("/characters/{id}")
	public ResponseEntity<Response> deleteCharacter(@PathVariable Long id) {
		characterService.delete(id);
		return Response.ok("Character deleted.");
	}

	@Operation(summary = "All products in any status. Paginated, searchable.")
	@PreAuthorize("hasAuthority('PRODUCT_READ_ALL')")
	@GetMapping("/products")
	public ResponseEntity<Response> products(
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String q,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize,
			@RequestParam(name = AppConstants.SORT_BY, defaultValue = AppConstants.SORT_BY_VALUE) String sortBy,
			@RequestParam(name = AppConstants.ASC_OR_DESC, defaultValue = AppConstants.ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
		ProductQueryService.Filter filter = new ProductQueryService.Filter(category, q, null, null, null, null);
		return Response.ok("Products loaded.", productQueryService.find(status, filter, pageNo, pageSize, sortBy, ascOrDesc));
	}

	@Operation(summary = "One product in any status, for the edit form.")
	@PreAuthorize("hasAuthority('PRODUCT_READ_ALL')")
	@GetMapping("/products/{id}")
	public ResponseEntity<Response> product(@PathVariable Long id) {
		return Response.ok("Product loaded.", productAdminService.get(id));
	}

	@Operation(summary = "Create a product with its options, facts, box items and images.")
	@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
	@PostMapping("/products")
	public ResponseEntity<Response> createProduct(@Valid @RequestBody ProductRequest request) {
		return Response.created("Product created.", productAdminService.create(request));
	}

	@Operation(summary = "Replace a product and its lists. Options keep their id when the value is unchanged.")
	@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
	@PutMapping("/products/{id}")
	public ResponseEntity<Response> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return Response.ok("Product updated.", productAdminService.update(id, request));
	}

	@Operation(summary = "Set stock for a product, or for one of its options.")
	@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
	@PatchMapping("/products/{id}/stock")
	public ResponseEntity<Response> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
		return Response.ok("Stock updated.", productAdminService.updateStock(id, request));
	}

	@Operation(summary = "Archive a product. It disappears from the shop; order history keeps it.")
	@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
	@DeleteMapping("/products/{id}")
	public ResponseEntity<Response> archiveProduct(@PathVariable Long id) {
		productAdminService.archive(id);
		return Response.ok("Product archived.");
	}
}
