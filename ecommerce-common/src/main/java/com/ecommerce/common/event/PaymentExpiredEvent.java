package com.ecommerce.common.event;

/**
 * An online payment was not completed in time. The order module cancels the order and puts
 * the stock back, in the same transaction.
 */
public record PaymentExpiredEvent(Long orderId, String orderNumber, String paymentReference) {}
