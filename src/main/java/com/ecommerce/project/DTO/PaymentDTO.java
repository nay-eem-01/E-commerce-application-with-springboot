package com.ecommerce.project.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long paymentId;
    private String paymentMethod;
    private String paymentGatewayId;
    private String paymentGatewayName;
    private String paymentGatewayStatus;
    private String paymentGatewayResponseMessage;

}