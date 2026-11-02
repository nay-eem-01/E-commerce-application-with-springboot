package com.ecommerce.order.enums;

/** Staff send an action, never a target status; the state machine decides if it is legal. */
public enum OrderAction {
	PACK,
	SHIP,
	DELIVER,
	CANCEL,
	RETURN
}
