package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.service.CategoryService;
import com.ecommerce.catalog.service.ProductQueryService;
import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.constant.AppConstants;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Public: browsing and search need no account. */
@Tag(name = "Storefront", description = "Categories, product lists, search and product pages")
@RestController
@CommonApiResponses
@RequestMapping("/api")
@RequiredArgsConstructor
public class StorefrontController {

	private final CategoryService categoryService;
	private final ProductQueryService productQueryService;
	private final com.ecommerce.catalog.service.CharacterService characterService;

	@Operation(summary = "Active categories with how many products each has.")
	@GetMapping("/categories")
	public ResponseEntity<Response> categories() {
		return Response.ok("Categories loaded.", categoryService.listActive());
	}

	@Operation(summary = "Active characters (the shop's collections) with how many products each has.")
	@GetMapping("/characters")
	public ResponseEntity<Response> characters() {
		return Response.ok("Characters loaded.", characterService.listActive());
	}

	@Operation(summary = "Filter groups and values (Capacity, Material, ...) with product counts, for a category and/or character.")
	@GetMapping("/products/facets")
	public ResponseEntity<Response> facets(
			@RequestParam(required = false) @Size(max = 60) String category,
			@RequestParam(required = false) @Size(max = 60) String character) {
		return Response.ok("Filters loaded.", productQueryService.facets(category, character));
	}

	@Operation(summary = "Product list and search. Filter by category or character slug, text, tags (\"Capacity:750 ml\", repeatable), stock, pre-order and price range; sort by creationDate, price or name.")
	@GetMapping("/products")
	public ResponseEntity<Response> products(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) @Size(max = 60) String character,
			@RequestParam(required = false) @Size(max = 20) java.util.List<@Size(max = 80) String> tag,
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) Boolean inStock,
			@RequestParam(required = false) Boolean preorder,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(name = AppConstants.PAGE_NO, defaultValue = AppConstants.PAGE_NO_VALUE) int pageNo,
			@RequestParam(name = AppConstants.PAGE_SIZE, defaultValue = AppConstants.PAGE_SIZE_VALUE) int pageSize,
			@RequestParam(name = AppConstants.SORT_BY, defaultValue = AppConstants.SORT_BY_VALUE) String sortBy,
			@RequestParam(name = AppConstants.ASC_OR_DESC, defaultValue = AppConstants.ASC_OR_DESC_VALUE) AscOrDescType ascOrDesc) {
		ProductQueryService.Filter filter = new ProductQueryService.Filter(category, q, inStock, preorder, minPrice, maxPrice,
				character, tag == null ? java.util.List.of() : tag);
		return Response.ok("Products loaded.", productQueryService.list(filter, pageNo, pageSize, sortBy, ascOrDesc));
	}

	@Operation(summary = "The product page: price, stock, options, specs, box contents, photos, 360 frames and story.")
	@GetMapping("/products/{slug}")
	public ResponseEntity<Response> product(@PathVariable String slug) {
		return Response.ok("Product loaded.", productQueryService.detail(slug));
	}

	@Operation(summary = "Up to four products to show under \"Goes well with\".")
	@GetMapping("/products/{slug}/related")
	public ResponseEntity<Response> related(@PathVariable String slug) {
		return Response.ok("Related products loaded.", productQueryService.related(slug));
	}
}
