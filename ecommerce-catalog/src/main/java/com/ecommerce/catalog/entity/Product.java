package com.ecommerce.catalog.entity;

import com.ecommerce.catalog.enums.MediaKind;
import com.ecommerce.catalog.enums.ProductStatus;
import com.ecommerce.catalog.enums.StockState;
import com.ecommerce.common.model.AuditModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock rule (one place): a product without options sells from {@code stock}. A product
 * with options sells from each option's stock, and {@code stock} holds their sum so lists
 * can filter and sort on one column. {@link #recomputeStock()} keeps the sum true.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends AuditModel {

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	/** The shop character this product belongs to, if any. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "character_id")
	private ShopCharacter character;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(length = 80)
	private String maker;

	@Column(nullable = false, length = 300)
	private String summary;

	/** Plain text, paragraphs separated by a blank line. Never HTML. */
	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String story;

	@Column(name = "story_quote", length = 200)
	private String storyQuote;

	/** The short spec under a card name, e.g. "24 cm, PVC and ABS". */
	@Column(name = "spec_line", length = 120)
	private String specLine;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "compare_at_price", precision = 12, scale = 2)
	private BigDecimal compareAtPrice;

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false)
	private boolean preorder;

	/** First day of the month the pre-order ships. */
	@Column(name = "preorder_release")
	private LocalDate preorderRelease;

	/** Paid now per unit for a pre-order; the rest is collected on delivery. */
	@Column(name = "deposit", precision = 12, scale = 2)
	private BigDecimal deposit;

	/** Shows the size guide (costumes). */
	@Column(nullable = false)
	private boolean sized;

	/** Label for the option group, e.g. "Size", "Colour", "Edition". Null = no options. */
	@Column(name = "option_name", length = 30)
	private String optionName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ProductStatus status = ProductStatus.DRAFT;

	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC, id ASC")
	@org.hibernate.annotations.BatchSize(size = 100)
	private List<ProductOption> options = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC, id ASC")
	private List<ProductFact> facts = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC, id ASC")
	private List<ProductBoxItem> boxItems = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("kind ASC, sortOrder ASC, id ASC")
	@org.hibernate.annotations.BatchSize(size = 100)
	private List<ProductMedia> media = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC, id ASC")
	private List<ProductTag> tags = new ArrayList<>();

	/** Listed in the last 30 days: the card shows "New". */
	public boolean isNewArrival(java.time.Instant now) {
		return getCreationDate() != null && getCreationDate().isAfter(now.minus(java.time.Duration.ofDays(30)));
	}

	public boolean hasOptions() {
		return !options.isEmpty();
	}

	public void recomputeStock() {
		if (hasOptions()) {
			stock = options.stream().mapToInt(ProductOption::getStock).sum();
		}
	}

	public StockState stockState() {
		return StockState.of(preorder, stock);
	}

	public String thumbnailUrl() {
		return media.stream().filter(m -> m.getKind() == MediaKind.PHOTO).map(ProductMedia::getUrl).findFirst()
				.orElse(media.stream().filter(m -> m.getKind() == MediaKind.SPIN_FRAME).map(ProductMedia::getUrl).findFirst().orElse(null));
	}
}
