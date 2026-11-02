package com.ecommerce.order.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.common.enums.AscOrDescType;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import com.ecommerce.order.config.DeliveryProperties;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.enums.OrderAction;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.ecommerce.payment.api.PaymentAction;
import com.ecommerce.payment.api.PaymentApi;
import com.ecommerce.payment.api.PaymentMethod;
import com.ecommerce.payment.api.PaymentStatus;
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
	private final PaymentApi payments = mock(PaymentApi.class);
	private final CatalogApi catalog = mock(CatalogApi.class);
	private final DeliveryProperties delivery = new DeliveryProperties(new BigDecimal("70"), new BigDecimal("130"),
			new BigDecimal("3000"), List.of("Dhaka"), List.of("Dhaka"), "1 to 2 days", "2 to 4 days");
	private final OrderService service = new OrderService(orders, mock(OrderStatusHistoryRepository.class), payments,
			new DeliveryFeeCalculator(delivery), catalog, new InMemoryRateLimiter(), Clock.systemUTC());

	private Order order(OrderStatus status, PaymentMethod method) {
		Order o = new Order();
		o.setId(1L);
		o.setOrderNumber("SS-ABCDEFGH");
		o.setStatus(status);
		o.setDistrict("Dhaka");
		o.setPaymentMethod(method);
		o.setPhone("+8801712345678");
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
	void cancellingPutsStockBackAndSettlesThePayment() {
		Order o = order(OrderStatus.PLACED, PaymentMethod.COD);
		when(orders.findByOrderNumberAndUserId("SS-ABCDEFGH", 2L)).thenReturn(Optional.of(o));
		service.cancelMine(2L, "SS-ABCDEFGH");
		assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		verify(catalog).release(anyList());
		verify(payments).settleCancellation(1L);
	}

	@Test
	void shoppersCannotCancelAPackedOrder() {
		when(orders.findByOrderNumberAndUserId("SS-ABCDEFGH", 2L)).thenReturn(Optional.of(order(OrderStatus.PACKED, PaymentMethod.COD)));
		assertThatThrownBy(() -> service.cancelMine(2L, "SS-ABCDEFGH"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
		verify(catalog, never()).release(anyList());
	}

	@Test
	void deliveringRecordsTheCashAsCollected() {
		Order o = order(OrderStatus.SHIPPED, PaymentMethod.COD);
		when(orders.findByOrderNumber("SS-ABCDEFGH")).thenReturn(Optional.of(o));
		when(payments.statusOf(1L)).thenReturn(PaymentStatus.PAID);
		var response = service.adminApply("SS-ABCDEFGH", OrderAction.DELIVER, null, 1L);
		verify(payments).markCashCollected(1L);
		assertThat(o.getDeliveredAt()).isNotNull();
		assertThat(response.amountDueOnDelivery()).isEqualByComparingTo("0.00");
	}

	@Test
	void anUnpaidOnlineOrderCannotBePacked() {
		when(orders.findByOrderNumber("SS-ABCDEFGH")).thenReturn(Optional.of(order(OrderStatus.PLACED, PaymentMethod.BKASH)));
		when(payments.isPaid(1L)).thenReturn(false);
		assertThatThrownBy(() -> service.adminApply("SS-ABCDEFGH", OrderAction.PACK, null, 1L))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aPaidOnlineOrderCanBePackedAndCodNeverWaits() {
		Order online = order(OrderStatus.PLACED, PaymentMethod.CARD);
		when(orders.findByOrderNumber("SS-ABCDEFGH")).thenReturn(Optional.of(online));
		when(payments.isPaid(1L)).thenReturn(true);
		service.adminApply("SS-ABCDEFGH", OrderAction.PACK, null, 1L);
		assertThat(online.getStatus()).isEqualTo(OrderStatus.PACKED);

		Order cod = order(OrderStatus.PLACED, PaymentMethod.COD);
		when(orders.findByOrderNumber("SS-ABCDEFGH")).thenReturn(Optional.of(cod));
		when(payments.isPaid(1L)).thenReturn(false);
		service.adminApply("SS-ABCDEFGH", OrderAction.PACK, null, 1L);
		assertThat(cod.getStatus()).isEqualTo(OrderStatus.PACKED);
	}

	@Test
	void retryingPaymentOnlyForUnpaidPlacedOnlineOrders() {
		Order o = order(OrderStatus.PLACED, PaymentMethod.BKASH);
		when(orders.findByOrderNumberAndPhone("SS-ABCDEFGH", "+8801712345678")).thenReturn(Optional.of(o));
		when(payments.startOnline(any())).thenReturn(PaymentAction.redirect("https://pay.example/1"));
		assertThat(service.retryPayment("ss-abcdefgh", "01712345678", "1.1.1.1").url()).isEqualTo("https://pay.example/1");

		when(payments.isPaid(1L)).thenReturn(true);
		assertThatThrownBy(() -> service.retryPayment("SS-ABCDEFGH", "01712345678", "1.1.1.1"))
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);

		Order cod = order(OrderStatus.PLACED, PaymentMethod.COD);
		when(orders.findByOrderNumberAndPhone(any(), any())).thenReturn(Optional.of(cod));
		assertThatThrownBy(() -> service.retryPayment("SS-ABCDEFGH", "01712345678", "1.1.1.1")).isInstanceOf(ResponseException.class);
	}

	@Test
	void anExpiredPaymentCancelsTheOrderAndRestocks() {
		Order o = order(OrderStatus.PLACED, PaymentMethod.BKASH);
		when(orders.findById(1L)).thenReturn(Optional.of(o));
		service.cancelUnpaid(1L, "PAY-X");
		assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		verify(catalog).release(anyList());
	}

	@Test
	void anExpiryEventForAPaidOrPackedOrderChangesNothing() {
		Order packed = order(OrderStatus.PACKED, PaymentMethod.BKASH);
		when(orders.findById(1L)).thenReturn(Optional.of(packed));
		service.cancelUnpaid(1L, "PAY-X");
		assertThat(packed.getStatus()).isEqualTo(OrderStatus.PACKED);
		verify(catalog, never()).release(anyList());
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
