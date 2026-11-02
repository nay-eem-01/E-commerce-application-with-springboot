package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.request.CartLineRequest;
import com.ecommerce.cart.dto.request.CartMergeRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.service.WishlistService;
import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.model.Response;
import com.ecommerce.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cart and wishlist", description = "The signed-in shopper's cart and saved products")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;
	private final WishlistService wishlistService;
	private final CurrentUser currentUser;

	@Operation(summary = "My cart, priced from the catalog right now.")
	@GetMapping("/api/cart")
	public ResponseEntity<Response> cart() {
		return Response.ok("Cart loaded.", cartService.get(currentUser.id()));
	}

	@Operation(summary = "Set the quantity of one product (and option) in my cart. Quantity 0 removes it.")
	@PutMapping("/api/cart/items")
	public ResponseEntity<Response> setLine(@Valid @RequestBody CartLineRequest request) {
		return Response.ok("Cart updated.", cartService.setLine(currentUser.id(), request));
	}

	@Operation(summary = "Remove one line from my cart.")
	@DeleteMapping("/api/cart/items/{itemId}")
	public ResponseEntity<Response> removeLine(@PathVariable Long itemId) {
		return Response.ok("Removed from cart.", cartService.removeLine(currentUser.id(), itemId));
	}

	@Operation(summary = "Add the browser's guest cart to my account cart after sign-in.")
	@PostMapping("/api/cart/merge")
	public ResponseEntity<Response> merge(@Valid @RequestBody CartMergeRequest request) {
		return Response.ok("Cart merged.", cartService.merge(currentUser.id(), request.getItems()));
	}

	@Operation(summary = "Empty my cart.")
	@DeleteMapping("/api/cart")
	public ResponseEntity<Response> clear() {
		cartService.clear(currentUser.id());
		return Response.ok("Cart emptied.");
	}

	@Operation(summary = "My saved products, newest first.")
	@GetMapping("/api/wishlist")
	public ResponseEntity<Response> wishlist() {
		return Response.ok("Wishlist loaded.", wishlistService.list(currentUser.id()));
	}

	@Operation(summary = "Ids of my saved products, to fill the hearts on product cards.")
	@GetMapping("/api/wishlist/ids")
	public ResponseEntity<Response> wishlistIds() {
		return Response.ok("Wishlist loaded.", wishlistService.productIds(currentUser.id()));
	}

	@Operation(summary = "Save a product to my wishlist.")
	@PutMapping("/api/wishlist/{productId}")
	public ResponseEntity<Response> addToWishlist(@PathVariable Long productId) {
		wishlistService.add(currentUser.id(), productId);
		return Response.ok("Saved to your wishlist.");
	}

	@Operation(summary = "Remove a product from my wishlist.")
	@DeleteMapping("/api/wishlist/{productId}")
	public ResponseEntity<Response> removeFromWishlist(@PathVariable Long productId) {
		wishlistService.remove(currentUser.id(), productId);
		return Response.ok("Removed from wishlist.");
	}
}
