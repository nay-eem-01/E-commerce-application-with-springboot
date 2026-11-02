package com.ecommerce.user.listener;

import com.ecommerce.common.event.OrderPlacedEvent;
import com.ecommerce.user.service.AddressService;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Runs inside the order transaction (see OrderPlacedEvent). Guests (no userId) are skipped. */
@Component
@RequiredArgsConstructor
public class OrderPlacedUserListener {

	private final AddressService addressService;
	private final UserService userService;

	@EventListener
	public void on(OrderPlacedEvent event) {
		if (event.userId() == null) {
			return;
		}
		OrderPlacedEvent.Address a = event.address();
		userService.fillNameIfMissing(event.userId(), a.fullName());
		if (event.saveAddress()) {
			addressService.saveFirstAddress(event.userId(), a.fullName(), a.phone(), a.district(), a.area(), a.addressLine());
		}
	}
}
