package com.ecommerce.catalog.dto.request;

import com.ecommerce.catalog.enums.MediaKind;
import com.ecommerce.catalog.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Create or fully replace a product, including its options, facts, box items and media. */
@Getter
@Setter
public class ProductRequest {

	/** Only http(s) URLs: a javascript: or data: URL would become XSS on the storefront. */
	public static final String URL_PATTERN = "^https?://[^\\s\"'<>]+$";

	@NotBlank(message = "Enter a slug.")
	@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Use lowercase letters, numbers and single hyphens for the slug.")
	@Size(max = 120, message = "Use at most 120 characters for the slug.")
	private String slug;

	@NotNull(message = "Choose a category.")
	private Long categoryId;

	/** The character collection, or null. */
	private Long characterId;

	@NotBlank(message = "Enter a name.")
	@Size(max = 140, message = "Use at most 140 characters for the name.")
	private String name;

	@Size(max = 80, message = "Use at most 80 characters for the maker.")
	private String maker;

	@NotBlank(message = "Enter a one-sentence summary.")
	@Size(max = 300, message = "Use at most 300 characters for the summary.")
	private String summary;

	@Size(max = 10_000, message = "The description is too long.")
	private String description;

	@Size(max = 10_000, message = "The story is too long.")
	private String story;

	@Size(max = 200, message = "Use at most 200 characters for the quote.")
	private String storyQuote;

	@Size(max = 120, message = "Use at most 120 characters for the spec line.")
	private String specLine;

	@NotNull(message = "Enter a price.")
	@DecimalMin(value = "1.00", message = "The price must be at least 1 taka.")
	@DecimalMax(value = "9999999.99", message = "The price is too large.")
	@Digits(integer = 10, fraction = 2, message = "Use at most two decimals for the price.")
	private BigDecimal price;

	@DecimalMin(value = "1.00", message = "The compare-at price must be at least 1 taka.")
	@Digits(integer = 10, fraction = 2, message = "Use at most two decimals for the compare-at price.")
	private BigDecimal compareAtPrice;

	/** Used only when the product has no options. */
	@Min(value = 0, message = "Stock cannot be negative.")
	@Max(value = 1_000_000, message = "Stock is too large.")
	private int stock;

	private boolean preorder;

	/** "2027-01". Required for a pre-order. */
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Use the format YYYY-MM for the release month.")
	private String preorderRelease;

	@DecimalMin(value = "0.00", message = "The deposit cannot be negative.")
	@Digits(integer = 10, fraction = 2, message = "Use at most two decimals for the deposit.")
	private BigDecimal deposit;

	private boolean sized;

	@NotNull(message = "Choose a status.")
	private ProductStatus status;

	@Size(max = 30, message = "Use at most 30 characters for the option name.")
	private String optionName;

	@Valid
	@Size(max = 30, message = "A product can have at most 30 options.")
	private List<OptionRequest> options = new ArrayList<>();

	@Valid
	@Size(max = 30, message = "A product can have at most 30 facts.")
	private List<FactRequest> facts = new ArrayList<>();

	@Size(max = 30, message = "A product can have at most 30 box items.")
	private List<@NotBlank @Size(max = 120) String> boxItems = new ArrayList<>();

	@Valid
	@Size(max = 60, message = "A product can have at most 60 images.")
	private List<MediaRequest> media = new ArrayList<>();

	/** Makes this product a set of these products (in this order). Empty for an ordinary product. */
	@Size(max = 10, message = "A set can hold at most 10 products.")
	private List<@jakarta.validation.constraints.NotNull Long> bundleProductIds = new ArrayList<>();

	@Valid
	@Size(max = 30, message = "A product can have at most 30 tags.")
	private List<TagRequest> tags = new ArrayList<>();

	@Getter
	@Setter
	public static class TagRequest {
		@NotBlank(message = "Enter the tag group, like Capacity.")
		@Size(max = 30, message = "Use at most 30 characters for a tag group.")
		@Pattern(regexp = "^[^:]+$", message = "A tag group cannot contain a colon.")
		private String group;
		@NotBlank(message = "Enter the tag value, like 750 ml.")
		@Size(max = 40, message = "Use at most 40 characters for a tag value.")
		private String value;
	}

	@Getter
	@Setter
	public static class OptionRequest {
		@NotBlank(message = "Enter the option value.")
		@Size(max = 60, message = "Use at most 60 characters for an option value.")
		private String value;

		@NotNull(message = "Enter the price difference (0 for none).")
		@DecimalMin(value = "0.00", message = "The price difference cannot be negative.")
		@Digits(integer = 10, fraction = 2, message = "Use at most two decimals for the price difference.")
		private BigDecimal priceDelta = BigDecimal.ZERO;

		@Min(value = 0, message = "Stock cannot be negative.")
		@Max(value = 1_000_000, message = "Stock is too large.")
		private int stock;

		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Use a hex colour like #f4a6c0 for the swatch.")
		private String swatch;
	}

	@Getter
	@Setter
	public static class FactRequest {
		@NotBlank(message = "Enter the fact label.")
		@Size(max = 40, message = "Use at most 40 characters for a fact label.")
		private String label;

		@NotBlank(message = "Enter the fact value.")
		@Size(max = 120, message = "Use at most 120 characters for a fact value.")
		private String value;
	}

	@Getter
	@Setter
	public static class MediaRequest {
		@NotNull(message = "Choose the image kind.")
		private MediaKind kind;

		@NotBlank(message = "Enter the image URL.")
		@Size(max = 500, message = "The image URL is too long.")
		@Pattern(regexp = URL_PATTERN, message = "Use an http or https image URL.")
		private String url;

		@Size(max = 160, message = "Use at most 160 characters for the alt text.")
		private String altText;
	}
}
