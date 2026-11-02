package com.ecommerce.order.service;

import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.order.enums.OrderAction;
import com.ecommerce.order.enums.OrderStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The only place that knows which order transitions are legal. An illegal one is 409.
 *
 * PLACED -> PACKED -> SHIPPED -> DELIVERED
 * PLACED | PACKED -> CANCELLED
 * SHIPPED (failed delivery) | DELIVERED (7-day return) -> RETURNED
 */
public final class OrderStateMachine {
	private OrderStateMachine() {}

	private record Rule(Set<OrderStatus> from, OrderStatus to) {}

	private static final Map<OrderAction, Rule> RULES = new EnumMap<>(Map.of(
			OrderAction.PACK, new Rule(Set.of(OrderStatus.PLACED), OrderStatus.PACKED),
			OrderAction.SHIP, new Rule(Set.of(OrderStatus.PACKED), OrderStatus.SHIPPED),
			OrderAction.DELIVER, new Rule(Set.of(OrderStatus.SHIPPED), OrderStatus.DELIVERED),
			OrderAction.CANCEL, new Rule(Set.of(OrderStatus.PLACED, OrderStatus.PACKED), OrderStatus.CANCELLED),
			OrderAction.RETURN, new Rule(Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED), OrderStatus.RETURNED)
	));

	public static OrderStatus next(OrderStatus current, OrderAction action) {
		Rule rule = RULES.get(action);
		if (rule == null || !rule.from().contains(current)) {
			throw ResponseException.conflict(ErrorCode.ILLEGAL_ORDER_TRANSITION);
		}
		return rule.to();
	}

	/** Shoppers may cancel online only before the order is packed. */
	public static boolean customerMayCancel(OrderStatus current) {
		return current == OrderStatus.PLACED;
	}
}
