package com.ecommerce.order.dto.request;

import com.ecommerce.order.enums.OrderAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderActionRequest {

	@NotNull(message = "Choose an action.")
	private OrderAction action;

	@Size(max = 200, message = "Use at most 200 characters for the note.")
	private String note;
}
