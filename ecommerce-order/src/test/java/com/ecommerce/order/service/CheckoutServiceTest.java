package com.ecommerce.order.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.api.PricedLine;
import com.ecommerce.catalog.enums.StockState;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.ratelimit.InMemoryRateLimiter;
import com.ecommerce.order.config.DeliveryProperties;
import com.ecommerce.order.config.PaymentProperties;
import com.ecommerce.order.dto.request.CheckoutLine;
import com.ecommerce.order.dto.request.PlaceOrderRequest;
import com.ecommerce.order.dto.response.QuoteResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.enums.PaymentMethod;
import com.ecommerce.order.payment.CashOnDeliveryGateway;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.ecommerce.order.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {

	private final CatalogApi catalog = mock(CatalogApi.class);
	private final OrderRepository orders = mock(OrderRepository.class);
	private final DeliveryProperties delivery = new DeliveryProperties(new BigDecimal("70"), new BigDecimal("130"),
			new BigDecimal("3000"), List.of("Dhaka"), List.of("Dhaka", "Sylhet"), "1 to 2 days", "2 to 4 days");
	private CheckoutService service;

	@BeforeEach
	void setUp() {
		service = new CheckoutService(catalog, new DeliveryFeeCalculator(delivery), delivery,
				new PaymentProperties(Set.of(PaymentMethod.COD)), List.of(new CashOnDeliveryGateway()),
				new OrderNumberGenerator(), orders, mock(OrderStatusHistoryRepository.class), mock(PaymentRepository.class),
				mock(ApplicationEventPublisher.class), new InMemoryRateLimiter(), Clock.systemUTC());
	}

	@Test
	void quoteUsesCatalogPricesAndDeposits() {
		QuoteResponse q = service.buildQuote(List.of(
				line(1L, "6800.00", 2, false, "0.00", 4),
				line(2L, "2950.00", 1, true, "1000.00", 40)), "Sylhet");
		assertThat(q.subtotal()).isEqualByComparingTo("16550.00");
		assertThat(q.deliveryFee()).isEqualByComparingTo("0.00");
		assertThat(q.total()).isEqualByComparingTo("16550.00");
		assertThat(q.depositTotal()).isEqualByComparingTo("1000.00");
		assertThat(q.allInStock()).isTrue();
	}

	@Test
	void quoteFlagsShortStockWithoutFailing() {
		// 2 x 1350 = 2700: under the 3000 threshold, so Dhaka pays 70 and is 300 away from free.
		QuoteResponse q = service.buildQuote(List.of(line(1L, "1350.00", 2, false, "0.00", 1)), "Dhaka");
		assertThat(q.allInStock()).isFalse();
		assertThat(q.deliveryFee()).isEqualByComparingTo("70.00");
		assertThat(q.total()).isEqualByComparingTo("2770.00");
		assertThat(q.amountToFreeDelivery()).isEqualByComparingTo("300.00");
	}

	@Test
	void duplicateLinesAreMergedAndCappedAtTen() {
		var merged = CheckoutService.merge(List.of(cl(5L, null, 6), cl(5L, null, 7), cl(5L, 9L, 1)));
		assertThat(merged).hasSize(2);
		assertThat(merged.getFirst().quantity()).isEqualTo(10);
	}

	@Test
	void missingOrMalformedIdempotencyKeyIsRejectedBeforeAnythingElse() {
		assertThatThrownBy(() -> service.place(request(), null, null, "1.1.1.1")).isInstanceOf(ResponseException.class);
		assertThatThrownBy(() -> service.place(request(), "short", null, "1.1.1.1")).isInstanceOf(ResponseException.class);
		verify(catalog, never()).reserve(any());
	}

	@Test
	void reusingSomeoneElsesIdempotencyKeyIs409AndTakesNoStock() {
		Order existing = new Order();
		existing.setUserId(42L);
		existing.setPhone("+8801712345678");
		when(orders.findByIdempotencyKey("key-1234567890abcdef")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.place(request(), "key-1234567890abcdef", 7L, "1.1.1.1"))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
		// a guest presenting a signed-in shopper's key does not see their order either
		assertThatThrownBy(() -> service.place(request(), "key-1234567890abcdef", null, "1.1.1.1"))
				.isInstanceOf(ResponseException.class);
		verify(catalog, never()).reserve(any());
	}

	@Test
	void disabledPaymentMethodIsRejectedBeforeStockIsTaken() {
		PlaceOrderRequest r = request();
		r.setPaymentMethod(PaymentMethod.BKASH);
		when(orders.findByIdempotencyKey(any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.place(r, "key-abcdefghijklmnop", null, "2.2.2.2")).isInstanceOf(ResponseException.class);
		verify(catalog, never()).reserve(any());
	}

	private static PricedLine line(Long id, String price, int qty, boolean preorder, String deposit, int stock) {
		return new PricedLine(id, null, "p" + id, "Product " + id, null, null, new BigDecimal(price), qty, preorder,
				new BigDecimal(deposit), preorder ? "2027-01" : null, stock, StockState.of(preorder, stock), null);
	}

	private static CheckoutLine cl(Long productId, Long optionId, int qty) {
		CheckoutLine l = new CheckoutLine();
		l.setProductId(productId);
		l.setOptionId(optionId);
		l.setQuantity(qty);
		return l;
	}

	private static PlaceOrderRequest request() {
		PlaceOrderRequest r = new PlaceOrderRequest();
		r.setItems(List.of(cl(1L, null, 1)));
		r.setFullName("Rafi Islam");
		r.setPhone("01712345678");
		r.setDistrict("Dhaka");
		r.setArea("Dhanmondi");
		r.setAddressLine("House 12, Road 5");
		r.setPaymentMethod(PaymentMethod.COD);
		return r;
	}
}
