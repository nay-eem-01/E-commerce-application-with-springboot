package com.ecommerce.order.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.money.Money;
import com.ecommerce.order.config.DeliveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** The one place delivery charges are computed. Free at or above the threshold. */
@Component
@RequiredArgsConstructor
public class DeliveryFeeCalculator {

	private final DeliveryProperties properties;

	public BigDecimal fee(String district, BigDecimal subtotal) {
		requireKnown(district);
		if (Money.of(subtotal).compareTo(Money.of(properties.freeThreshold())) >= 0) {
			return Money.ZERO;
		}
		return Money.of(isInside(district) ? properties.insideFee() : properties.outsideFee());
	}

	public boolean isInside(String district) {
		return properties.insideDistricts().stream().anyMatch(d -> d.equalsIgnoreCase(district));
	}

	public String estimate(String district) {
		return isInside(district) ? properties.insideEstimate() : properties.outsideEstimate();
	}

	/** Returns the district as configured (canonical spelling) or throws 400. */
	public String requireKnown(String district) {
		return properties.districts().stream().filter(d -> d.equalsIgnoreCase(district == null ? "" : district.trim()))
				.findFirst().orElseThrow(() -> ResponseException.badRequest(ErrorCode.UNKNOWN_DISTRICT));
	}
}
