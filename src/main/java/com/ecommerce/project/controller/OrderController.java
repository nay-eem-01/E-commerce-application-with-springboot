package com.ecommerce.project.controller;

import com.ecommerce.project.DTO.OrderDTO;
import com.ecommerce.project.DTO.OrderRequestDTO;
import com.ecommerce.project.Util.AuthUtil;
import com.ecommerce.project.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;
    private final AuthUtil authUtil;

    public OrderController(OrderService orderService, AuthUtil authUtil) {
        this.orderService = orderService;
        this.authUtil = authUtil;
    }

    @PostMapping("/order/place/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod, @RequestBody OrderRequestDTO orderRequestDTO){
        String email = authUtil.LoggedInEmail();


        OrderDTO orderDTO = orderService.placeOrder(
                email,
                paymentMethod,
                orderRequestDTO.getAddressId(),
                orderRequestDTO.getPaymentGatewayId(),
                orderRequestDTO.getPaymentGatewayName(),
                orderRequestDTO.getPaymentGatewayStatus(),
                orderRequestDTO.getPaymentGatewayResponseMessage()

        );

        return  new ResponseEntity<>(orderDTO, HttpStatus.CREATED);

    }
}
