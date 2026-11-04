package com.ecommerce.catalog.service;

import com.ecommerce.catalog.api.CatalogApi;
import com.ecommerce.catalog.api.LineRequest;
import com.ecommerce.catalog.api.PricedLine;
import com.ecommerce.catalog.dto.response.ProductSummaryResponse;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.enums.StockState;
import com.ecommerce.catalog.repository.ProductOptionRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.constant.ErrorCode;
import com.ecommerce.common.exception.ResponseException;
import com.ecommerce.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogApiImpl implements CatalogApi {

	private final ProductRepository productRepository;
	private final ProductOptionRepository optionRepository;

	/**
	 * noRollbackFor: callers (cart merge, cart view) catch the "unavailable" exception to skip a
	 * line. Without it the exception would mark the caller's whole transaction rollback-only.
	 */
	@Override
	@Transactional(readOnly = true, noRollbackFor = ResponseException.class)
	public List<PricedLine> price(List<LineRequest> lines) {
		Map<Long, Product> products = load(lines);
		List<PricedLine> priced = new ArrayList<>();
		for (LineRequest line : lines) {
			Product p = products.get(line.productId());
			if (p == null || p.getStatus() != ProductStatus.ACTIVE) {
				throw ResponseException.notFound(ErrorCode.PRODUCT_UNAVAILABLE);
			}
			ProductOption option = optionFor(p, line.optionId());
			int available = option != null ? option.getStock() : p.getStock();
			priced.add(new PricedLine(
					p.getId(), option == null ? null : option.getId(), p.getSlug(), p.getName(),
					option == null ? null : p.getOptionName(), option == null ? null : option.getValue(),
					option == null ? p.getPrice() : Money.add(p.getPrice(), option.getPriceDelta()),
					line.quantity(), p.isPreorder(), p.isPreorder() ? Money.of(p.getDeposit()) : Money.ZERO,
					p.getPreorderRelease() == null ? null : YearMonth.from(p.getPreorderRelease()).toString(),
					available, StockState.of(p.isPreorder(), available), p.thumbnailUrl()
			));
		}
		return priced;
	}

	/**
	 * Joins the caller's transaction so stock and the order commit or roll back together.
	 * Lines are applied in product-id order so two concurrent orders lock rows in the same
	 * order and cannot deadlock.
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void reserve(List<LineRequest> lines) {
		List<Long> short_ = new ArrayList<>();
		for (LineRequest line : sorted(lines)) {
			boolean ok;
			if (line.optionId() != null) {
				ok = optionRepository.takeStock(line.productId(), line.optionId(), line.quantity()) == 1
						&& productRepository.takeStock(line.productId(), line.quantity()) == 1;
			} else {
				ok = productRepository.takeStock(line.productId(), line.quantity()) == 1;
			}
			if (!ok) {
				short_.add(line.productId());
			}
		}
		if (!short_.isEmpty()) {
			// Thrown inside the caller's transaction: every decrement above rolls back.
			throw new ResponseException(HttpStatus.CONFLICT, ErrorCode.OUT_OF_STOCK, Map.of("productIds", short_));
		}
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void release(List<LineRequest> lines) {
		for (LineRequest line : sorted(lines)) {
			if (line.optionId() != null) {
				optionRepository.returnStock(line.productId(), line.optionId(), line.quantity());
			}
			productRepository.returnStock(line.productId(), line.quantity());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Map<Long, String> names(Collection<Long> productIds) {
		if (productIds.isEmpty()) return java.util.Map.of();
		return productRepository.findByIdIn(productIds).stream()
				.collect(java.util.stream.Collectors.toMap(p -> p.getId(), p -> p.getName()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductSummaryResponse> summaries(Collection<Long> productIds) {
		if (productIds.isEmpty()) {
			return List.of();
		}
		return productRepository.findByIdIn(productIds).stream()
				.filter(p -> p.getStatus() == ProductStatus.ACTIVE)
				.map(ProductSummaryResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> activeProductIdBySlug(String slug) {
		return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE).map(Product::getId);
	}

	private Map<Long, Product> load(List<LineRequest> lines) {
		return productRepository.findByIdIn(lines.stream().map(LineRequest::productId).distinct().toList())
				.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
	}

	private static ProductOption optionFor(Product p, Long optionId) {
		if (!p.hasOptions()) {
			if (optionId != null) {
				throw ResponseException.badRequest(ErrorCode.OPTION_NOT_FOUND);
			}
			return null;
		}
		if (optionId == null) {
			throw ResponseException.badRequest(ErrorCode.OPTION_REQUIRED);
		}
		return p.getOptions().stream().filter(o -> o.getId().equals(optionId)).findFirst()
				.orElseThrow(() -> ResponseException.badRequest(ErrorCode.OPTION_NOT_FOUND));
	}

	private static List<LineRequest> sorted(List<LineRequest> lines) {
		return lines.stream().sorted(Comparator.comparing(LineRequest::productId)
				.thenComparing(l -> l.optionId() == null ? 0L : l.optionId())).toList();
	}
}
