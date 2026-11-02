package com.ecommerce.notification;

import com.ecommerce.common.event.OrderNotificationEvent;
import com.ecommerce.notification.config.MailProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Fixtures {
	private Fixtures() {}

	public static MailProperties props(String provider, String staff) {
		return new MailProperties(provider, "Senpai Supply <orders@shop.test>", "Senpai Supply", "https://shop.test", staff,
				Duration.ofSeconds(15), Duration.ofDays(30));
	}

	public static OrderNotificationEvent event(OrderNotificationEvent.Type type, String email, String method, boolean online, String name) {
		return new OrderNotificationEvent(type, new OrderNotificationEvent.OrderSummary("SS-7K2M9QXA", name, email, method, online,
				List.of(new OrderNotificationEvent.Line("Kitsune mask <cosplay> set", "Size: M", 2, new BigDecimal("8400.00"))),
				new BigDecimal("8400.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("8400.00"), "Dhaka", "1 to 2 days",
				Instant.parse("2026-11-02T10:00:00Z")), null);
	}
}
