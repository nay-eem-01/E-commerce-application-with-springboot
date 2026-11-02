package com.ecommerce.order.service;

import com.ecommerce.order.api.PurchaseVerifier;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseVerifierImpl implements PurchaseVerifier {

	private final OrderRepository orderRepository;

	@Override
	@Transactional(readOnly = true)
	public boolean hasReceived(Long userId, Long productId) {
		return orderRepository.hasReceived(userId, productId);
	}
}
