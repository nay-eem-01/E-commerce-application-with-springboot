package com.ecommerce.order.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import com.ecommerce.order.config.DeliveryProperties;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentMethod;
import com.ecommerce.order.enums.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.ecommerce.order.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

	private final OrderRepository orders = mock(OrderRepository.class);
	private final PaymentRepository payments = mock(PaymentRepository.class);
	private final CatalogApi catalog = mock(CatalogApi.class);
	private final DeliveryProperties delivery = new DeliveryProperties(new BigDecimal("70"), new BigDecimal("130"),
			new BigDecimal("3000"), List.of("Dhaka"), List.of("Dhaka"), "1 to 2 days", "2 to 4 days");
	private final OrderService service = new OrderService(orders, mock(OrderStatusHistoryRepository.class), payments,
			new DeliveryFeeCalculator(delivery), catalog, new InMemoryRateLimiter(), Clock.systemUTC());

	private Order order(OrderStatus status) {
		Order o = new Order();
		o.setId(1L);
		o.setOrderNumber("SS-ABCDEFGH");
		o.setStatus(status);
		o.setDistrict("Dhaka");
		o.setPaymentMethod(PaymentMethod.COD);
		o.setTotal(new BigDecimal("1420.00"));
		OrderItem i = new OrderItem();
		i.setProductId(3L);
		i.setQuantity(1);
		o.getItems().add(i);
		return o;
	}

	@Test
	void anotherShoppersOrderIsNotFound() {
		when(orders.findByOrderNumberAndUserId("SS-ABCDEFGH", 2L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.myOrder(2L, "ss-abcdefgh"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void cancellingPutsStockBackAndVoidsThePayment() {
		Order o = order(OrderStatus.PLACED);
		Payment p = new Payment();
		p.setMethod(PaymentMethod.COD);
		p.setStatus(PaymentStatus.PENDING);
		when(orders.findByOrderNumberAndUserId("SS-ABCDEFGH", 2L)).thenReturn(Optional.of(o));
		when(payments.findFirstByOrderIdOrderByIdDesc(1L)).thenReturn(Optional.of(p));

		service.cancelMine(2L, "SS-ABCDEFGH");
		assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(p.getStatus()).isEqualTo(PaymentStatus.VOIDED);
		verify(catalog).release(anyList());
	}

	@Test
	void shoppersCannotCancelAPackedOrder() {
		when(orders.findByOrderNumberAndUserId("SS-ABCDEFGH", 2L)).thenReturn(Optional.of(order(OrderStatus.PACKED)));
		assertThatThrownBy(() -> service.cancelMine(2L, "SS-ABCDEFGH"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
		verify(catalog, never()).release(anyList());
	}

	@Test
	void deliveringACodOrderMarksItPaid() {
		Order o = order(OrderStatus.SHIPPED);
		Payment p = new Payment();
		p.setMethod(PaymentMethod.COD);
		p.setStatus(PaymentStatus.PENDING);
		when(orders.findByOrderNumber("SS-ABCDEFGH")).thenReturn(Optional.of(o));
		when(payments.findFirstByOrderIdOrderByIdDesc(1L)).thenReturn(Optional.of(p));

		var response = service.adminApply("SS-ABCDEFGH", com.ecommerce.order.enums.OrderAction.DELIVER, null, 1L);
		assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(o.getDeliveredAt()).isNotNull();
		assertThat(response.amountDueOnDelivery()).isEqualByComparingTo("0.00");
	}

	/** Regression: a null search reached PostgreSQL as bytea inside upper() and the admin list 500'd. */
	@Test
	void adminSearchBindsAnEmptyStringNeverNull() {
		when(orders.search(any(), any(), any())).thenReturn(Page.empty());
		service.search(null, null, 0, 20, "placedAt", AscOrDescType.desc);
		verify(orders).search(any(), eq(""), any());
	}

	@Test
	void trackingWithTheWrongPhoneLooksExactlyLikeAMissingOrder() {
		when(orders.findByOrderNumberAndPhone(any(), any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.track("SS-ABCDEFGH", "01811111111", "1.1.1.1"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThatThrownBy(() -> service.track("SS-ABCDEFGH", "not-a-phone", "1.1.1.1"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
