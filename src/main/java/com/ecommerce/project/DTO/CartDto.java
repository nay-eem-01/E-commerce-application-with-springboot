package com.ecommerce.project.DTO;

import com.ecommerce.project.model.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private Long cartId;
    private double totalPrice;
    private List<ProductDTO> productDTOS = new ArrayList<>();
}
