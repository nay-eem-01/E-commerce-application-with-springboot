package com.ecommerce.project.service;


import com.ecommerce.project.DTO.OrderDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
   @Transactional
   OrderDTO placeOrder(String email, String paymentMethod, Long addressId, Long paymentGatewayId, String paymentGatewayName, String paymentGatewayStatus, String paymentGatewayResponseMessage);
}
