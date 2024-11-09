package com.ecommerce.project.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private Long addressId;
    private String paymentMethod;
    private String paymentGatewayName;
    private Long paymentGatewayId;
    private String paymentGatewayStatus;
    private String paymentGatewayResponseMessage;
}