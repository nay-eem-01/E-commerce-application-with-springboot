package com.ecommerce.common.time;

import java.time.ZoneId;

/** The shop's calendar: days, reports and counters are Bangladesh dates. */
public final class ShopTime {
	private ShopTime() {}

	public static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");
}
