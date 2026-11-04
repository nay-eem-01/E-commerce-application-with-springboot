package com.ecommerce.insights.dto.response;

import java.math.BigDecimal;

/** {@code averageOrder} is null without orders; {@code cancelRate} is cancelled / (counted + cancelled), 0 to 1. */
public record TotalsResponse(long orders, long units, BigDecimal revenue, BigDecimal averageOrder, BigDecimal deliveryFees,
							 long cancelled, BigDecimal cancelRate, long returned, long newBuyers, long returningBuyers,
							 long views, long searches, long zeroResultSearches) {}
