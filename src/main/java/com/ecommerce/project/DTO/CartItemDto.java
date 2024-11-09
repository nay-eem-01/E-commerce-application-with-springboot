package com.ecommerce.project.DTO;

import com.ecommerce.project.model.Cart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long cartItemId;
    private Integer quantity;
    private double discount;
    private double price;
    private CartDto cartDto;
    private ProductDTO productDTO;
}
