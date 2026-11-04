package com.ecommerce.catalog.dto.response;

import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.enums.MediaKind;
import com.ecommerce.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * A product in exactly the shape the admin editor saves ({@code ProductRequest}): ids instead of names, exact stock,
 * media with kind in saved order. Staff only; the shop's own responses never show exact counts.
 */
public record ProductEditResponse(Long id, String slug, Long categoryId, Long characterId, String name, String maker,
								  String summary, String description, String story, String storyQuote, String specLine,
								  BigDecimal price, BigDecimal compareAtPrice, int stock, boolean preorder,
								  String preorderRelease, BigDecimal deposit, boolean sized, ProductStatus status,
								  String optionName, List<Option> options, List<Fact> facts, List<String> boxItems,
								  List<Media> media, List<Long> bundleProductIds, List<Tag> tags) {

	public record Option(Long id, String value, BigDecimal priceDelta, int stock, String swatch) {}

	public record Fact(String label, String value) {}

	public record Media(MediaKind kind, String url, String altText) {}

	public record Tag(String group, String value) {}

	public static ProductEditResponse from(Product p) {
		return new ProductEditResponse(p.getId(), p.getSlug(), p.getCategory().getId(),
				p.getCharacter() == null ? null : p.getCharacter().getId(), p.getName(), p.getMaker(), p.getSummary(),
				p.getDescription(), p.getStory(), p.getStoryQuote(), p.getSpecLine(), p.getPrice(), p.getCompareAtPrice(),
				p.getStock(), p.isPreorder(),
				p.getPreorderRelease() == null ? null : java.time.YearMonth.from(p.getPreorderRelease()).toString(),
				p.getDeposit(), p.isSized(), p.getStatus(), p.getOptionName(),
				p.getOptions().stream().map(o -> new Option(o.getId(), o.getValue(), o.getPriceDelta(), o.getStock(), o.getSwatch())).toList(),
				p.getFacts().stream().map(f -> new Fact(f.getLabel(), f.getValue())).toList(),
				p.getBoxItems().stream().map(b -> b.getItem()).toList(),
				p.getMedia().stream().map(m -> new Media(m.getKind(), m.getUrl(), m.getAltText())).toList(),
				p.getBundleItems().stream().map(b -> b.getComponent().getId()).toList(),
				p.getTags().stream().map(t -> new Tag(t.getGroup(), t.getValue())).toList());
	}
}
