package com.ecommerce.project.service;

import com.ecommerce.project.DTO.AddressDto;
import com.ecommerce.project.DTO.OrderDTO;
import com.ecommerce.project.DTO.OrderItemDTO;
import com.ecommerce.project.exceptionHandler.APIExceptionHandler;
import com.ecommerce.project.exceptionHandler.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    private final CartRepositories cartRepositories;
    private final AddressRepository addressRepository;
    private final PaymentRepositories paymentRepositories;
    private final OrderRepositories orderRepositories;
    private final OrderItemRepositories orderItemRepositories;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;

    public OrderServiceImpl(CartRepositories cartRepositories, AddressRepository addressRepository, PaymentRepositories paymentRepositories, OrderRepositories orderRepositories, OrderItemRepositories orderItemRepositories, ProductRepository productRepository, CartService cartService, ModelMapper modelMapper) {
        this.cartRepositories = cartRepositories;
        this.addressRepository = addressRepository;
        this.paymentRepositories = paymentRepositories;
        this.orderRepositories = orderRepositories;
        this.orderItemRepositories = orderItemRepositories;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.modelMapper = modelMapper;
    }




    @Override
    @Transactional
    public OrderDTO placeOrder(String email, String paymentMethod, Long addressId, Long paymentGatewayId, String paymentGatewayName, String paymentGatewayStatus, String paymentGatewayResponseMessage) {

        Cart cart = cartRepositories.findByEmailId(email);

        if (cart == null){
            throw new ResourceNotFoundException("Cart","email", email);
        }

        Addresses addresses = addressRepository.findById(addressId).orElseThrow(()-> new ResourceNotFoundException("Address","addressId",addressId));

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()){
            throw  new APIExceptionHandler("Cart is empty");
        }

        Order order = new Order();
        order.setEmail(email);
        order.setOrderTime(LocalDate.now());
        order.setOrderStatus("Order Accepted");
        order.setAddresses(addresses);
        order.setTotalAmount(cart.getTotalPrice());


        Payment payment = new Payment(paymentMethod,paymentGatewayName,paymentGatewayId,paymentGatewayStatus,paymentGatewayResponseMessage);
        payment.setOrder(order);
        payment= paymentRepositories.save(payment);
        order.setPayment(payment);


        Order savedOrder = orderRepositories.save(order);

        List<OrderItem>orderItems= new ArrayList<>();
        for (CartItem cartItem : cartItems){
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cart.getTotalPrice());

            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }
        orderItems = orderItemRepositories.saveAll(orderItems);





        cart.getCartItems().forEach(cartItem -> {
            int quantity = cartItem.getQuantity();
            Product product = cartItem.getProduct();

            product.setQuantity(product.getQuantity()-quantity);

            productRepository.save(product);

        cartService.deleteProductFromCart(cart.getCartId(), product.getProductId());

        });

        OrderDTO orderDTO = modelMapper.map(savedOrder,OrderDTO.class);
        orderItems.forEach(orderItem ->
                orderDTO.getOrderItems().add(modelMapper.map(orderItem, OrderItemDTO.class)));

        orderDTO.setAddressId(addressId);
        orderDTO.setAddressDto(modelMapper.map(addresses, AddressDto.class));
        return orderDTO;

    }
}
