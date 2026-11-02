package com.ecommerce.order.service;

import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.order.enums.OrderAction;
import com.ecommerce.order.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

	@ParameterizedTest
	@CsvSource({
			"PLACED, PACK, PACKED",
			"PACKED, SHIP, SHIPPED",
			"SHIPPED, DELIVER, DELIVERED",
			"PLACED, CANCEL, CANCELLED",
			"PACKED, CANCEL, CANCELLED",
			"SHIPPED, RETURN, RETURNED",
			"DELIVERED, RETURN, RETURNED"
	})
	void everyLegalTransition(OrderStatus from, OrderAction action, OrderStatus to) {
		assertThat(OrderStateMachine.next(from, action)).isEqualTo(to);
	}

	@ParameterizedTest
	@CsvSource({
			"PLACED, DELIVER", "PLACED, SHIP", "PLACED, RETURN",
			"SHIPPED, CANCEL", "DELIVERED, CANCEL", "DELIVERED, PACK",
			"CANCELLED, PACK", "CANCELLED, DELIVER", "RETURNED, SHIP", "RETURNED, CANCEL"
	})
	void illegalTransitionsAre409(OrderStatus from, OrderAction action) {
		assertThatThrownBy(() -> OrderStateMachine.next(from, action))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void shoppersCanCancelOnlyBeforePacking() {
		assertThat(OrderStateMachine.customerMayCancel(OrderStatus.PLACED)).isTrue();
		for (OrderStatus s : OrderStatus.values()) {
			if (s != OrderStatus.PLACED) {
				assertThat(OrderStateMachine.customerMayCancel(s)).as(s.name()).isFalse();
			}
		}
	}
}
