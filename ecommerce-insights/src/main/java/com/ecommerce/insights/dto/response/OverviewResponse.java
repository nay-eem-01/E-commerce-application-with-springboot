package com.ecommerce.insights.dto.response;

import com.ecommerce.order.api.SalesReport.DailySales;

import java.time.LocalDate;
import java.util.List;

/** {@code daily} has one row per day of the period, zero rows included. */
public record OverviewResponse(LocalDate from, LocalDate to, TotalsResponse current, TotalsResponse previous,
							   List<DailySales> daily) {}
