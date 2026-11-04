package com.ecommerce.insights.controller;

import com.ecommerce.common.config.CommonApiResponses;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.model.Response;
import com.ecommerce.insights.enums.BreakdownBy;
import com.ecommerce.insights.service.InsightsService;
import com.ecommerce.insights.service.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/** Every endpoint takes an optional period: {@code from}/{@code to} shop dates, both inclusive, default the last 30 days. */
@Tag(name = "Admin: insights", description = "Sales, demand signals, breakdowns and search terms")
@RestController
@CommonApiResponses
@SecurityRequirement(name = "jwtToken")
@RequestMapping("/api/admin/insights")
@PreAuthorize("hasAuthority('INSIGHTS_READ')")
@RequiredArgsConstructor
public class AdminInsightsController {

	private final InsightsService insights;
	private final Clock clock;

	@Operation(summary = "Totals with the previous period of the same length, and a daily series.")
	@GetMapping("/overview")
	public ResponseEntity<Response> overview(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return Response.ok("Overview loaded.", insights.overview(Period.of(from, to, clock)));
	}

	@Operation(summary = "Every active product with sales, views, demand, stock, days left and its signals. Sort: revenue, units, views, conversion, wishlists, daysLeft, stock.")
	@GetMapping("/products")
	public ResponseEntity<Response> products(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "revenue") String sort,
			@RequestParam(defaultValue = "desc") AscOrDescType dir) {
		return Response.ok("Products loaded.", insights.products(Period.of(from, to, clock), sort, dir));
	}

	@Operation(summary = "Ranked demand signals: restock soon, wanted but sold out, looked at but not bought, slow movers, searches that found nothing, coming-soon demand.")
	@GetMapping("/signals")
	public ResponseEntity<Response> signals(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return Response.ok("Signals loaded.", insights.signals(Period.of(from, to, clock)));
	}

	@Operation(summary = "Sales by category, character, district or payment method.")
	@GetMapping("/breakdown")
	public ResponseEntity<Response> breakdown(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "CATEGORY") BreakdownBy by) {
		return Response.ok("Breakdown loaded.", insights.breakdown(Period.of(from, to, clock), by));
	}

	@Operation(summary = "The 20 most searched terms and the 20 that most often found nothing.")
	@GetMapping("/searches")
	public ResponseEntity<Response> searches(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return Response.ok("Searches loaded.", insights.searches(Period.of(from, to, clock)));
	}
}
