package com.ecommerce.project.controller;

import com.ecommerce.project.DTO.CartDto;
import com.ecommerce.project.Util.AuthUtil;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.repositories.CartRepositories;
import com.ecommerce.project.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CartRepositories cartRepositories;
    private final AuthUtil authUtil;

    public CartController(CartService cartService, CartRepositories cartRepositories, AuthUtil authUtil) {
        this.cartService = cartService;
        this.cartRepositories = cartRepositories;
        this.authUtil = authUtil;
    }

    @PostMapping("/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDto> addProductToCart(@PathVariable Long productId, @PathVariable Integer quantity){
        CartDto cartDto = cartService.addProductInCart(productId,quantity);
        return new ResponseEntity<>(cartDto, HttpStatus.CREATED);
    }
    @GetMapping("/all/carts")
    public ResponseEntity<List<CartDto>> getAllCarts(){

        List<CartDto> cartDtos = cartService.getAllCarts();
        return new ResponseEntity<>(cartDtos,HttpStatus.FOUND);
    }
    @GetMapping("/users/cart")
    public ResponseEntity<CartDto> getUserCart(){

        String email = authUtil.LoggedInEmail();
        Cart cart = cartRepositories.findByEmailId(email);
        Long cartId = cart.getCartId();
        CartDto cartDto = cartService.getUserCart(cartId,email);
        return new ResponseEntity<>(cartDto,HttpStatus.FOUND);
    }
    @PutMapping("/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDto> updateProductInCart(@PathVariable Long productId,@PathVariable String operation){

        CartDto cartDto = cartService.updateProductQuantity(productId,
                operation.equalsIgnoreCase("delete") ? -1 : 1);
        return new ResponseEntity<>(cartDto,HttpStatus.CREATED);
    }

    @DeleteMapping("/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId, @PathVariable Long productId){
         String status = cartService.deleteProductFromCart(cartId,productId);
        return new ResponseEntity<>(status,HttpStatus.OK);
    }
}

