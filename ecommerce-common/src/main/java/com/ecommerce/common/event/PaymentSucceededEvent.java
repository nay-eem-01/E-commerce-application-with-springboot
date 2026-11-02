package com.ecommerce.common.event;

import java.math.BigDecimal;

/** A payment was confirmed by its provider (or COD was collected). Published in the payment's transaction. */
public record PaymentSucceededEvent(Long orderId, String orderNumber, String paymentReference, String method, BigDecimal amount) {}
