package com.ecommerce.insights.dto.response;

import java.math.BigDecimal;

/** {@code share} is this row's part of the revenue, 0 to 1. */
public record BreakdownRowResponse(String name, long orders, long units, BigDecimal revenue, BigDecimal share) {}
