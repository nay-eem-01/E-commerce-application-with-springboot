package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.ProductMedia;
import com.ecommerce.catalog.entity.ProductOption;
import com.ecommerce.catalog.enums.MediaKind;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.enums.StockState;
import com.ecommerce.common.money.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The product page. Text fields are plain text (paragraphs split on a blank line); the
 * frontend renders them as text, never as HTML.
 */
public record ProductDetailResponse(
		Long id,
		String slug,
		String name,
		String maker,
		CategoryRef category,
		String summary,
		String description,
		String story,
		String storyQuote,
		String specLine,
		BigDecimal price,
		BigDecimal compareAtPrice,
		StockState stockState,
		Integer stockLeft,
		boolean preorder,
		String preorderRelease,
		BigDecimal deposit,
		boolean sized,
		String optionName,
		List<OptionResponse> options,
		List<FactResponse> facts,
		List<String> boxItems,
		List<MediaResponse> photos,
		List<String> spinFrames,
		MediaResponse storyImage,
		ProductStatus status,
		/** The character collection, or null. */
		CharacterRef character,
		/** Filterable attributes, e.g. Capacity: 750 ml. */
		List<TagResponse> tags,
		boolean newArrival
) {
	public record CategoryRef(String slug, String name) {}
	public record CharacterRef(String slug, String name, String label) {}
	public record TagResponse(String group, String value) {}

	public record OptionResponse(Long id, String value, BigDecimal priceDelta, BigDecimal price, StockState stockState, Integer stockLeft, String swatch) {}

	public record FactResponse(String label, String value) {}

	public record MediaResponse(String url, String altText) {}

	public static ProductDetailResponse from(Product p) {
		StockState state = p.stockState();
		List<ProductMedia> media = p.getMedia();
		return new ProductDetailResponse(
				p.getId(), p.getSlug(), p.getName(), p.getMaker(),
				new CategoryRef(p.getCategory().getSlug(), p.getCategory().getName()),
				p.getSummary(), p.getDescription(), p.getStory(), p.getStoryQuote(), p.getSpecLine(),
				p.getPrice(), p.getCompareAtPrice(), state,
				state == StockState.LOW_STOCK ? p.getStock() : null,
				p.isPreorder(), month(p.getPreorderRelease()), p.isPreorder() ? p.getDeposit() : null,
				p.isSized(), p.getOptionName(),
				p.getOptions().stream().map(o -> option(p, o)).toList(),
				p.getFacts().stream().map(f -> new FactResponse(f.getLabel(), f.getValue())).toList(),
				p.getBoxItems().stream().map(b -> b.getItem()).toList(),
				media.stream().filter(m -> m.getKind() == MediaKind.PHOTO).map(m -> new MediaResponse(m.getUrl(), m.getAltText())).toList(),
				media.stream().filter(m -> m.getKind() == MediaKind.SPIN_FRAME).map(ProductMedia::getUrl).toList(),
				media.stream().filter(m -> m.getKind() == MediaKind.STORY).findFirst().map(m -> new MediaResponse(m.getUrl(), m.getAltText())).orElse(null),
				p.getStatus(),
				p.getCharacter() == null ? null
						: new CharacterRef(p.getCharacter().getSlug(), p.getCharacter().getName(), p.getCharacter().getLabel()),
				p.getTags().stream().map(t -> new TagResponse(t.getGroup(), t.getValue())).toList(),
				p.isNewArrival(java.time.Instant.now())
		);
	}

	private static OptionResponse option(Product p, ProductOption o) {
		StockState state = StockState.of(p.isPreorder(), o.getStock());
		return new OptionResponse(o.getId(), o.getValue(), o.getPriceDelta(), Money.add(p.getPrice(), o.getPriceDelta()),
				state, state == StockState.LOW_STOCK ? o.getStock() : null, o.getSwatch());
	}

	/** "2027-01" - the frontend formats it as "January 2027". */
	static String month(LocalDate date) {
		return date == null ? null : YearMonth.from(date).toString();
	}
}
