package com.ecommerce.project.service;

import com.ecommerce.project.DTO.CartDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface CartService {
    CartDto addProductInCart( Long productId, Integer quantity);

    List<CartDto> getAllCarts();

    CartDto getUserCart(Long cartId, String email);
    @Transactional
    CartDto updateProductQuantity(Long productId, Integer quantity);

    String deleteProductFromCart(Long cartID, Long productID);

//    void updateProductInCarts(Long cartID, Long productID);
}
