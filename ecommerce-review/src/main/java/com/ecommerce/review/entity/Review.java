package com.ecommerce.review.entity;

import com.ecommerce.common.model.AuditModel;
import com.ecommerce.review.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One review per shopper per product, only after delivery, so every review is from a real buyer. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reviews")
public class Review extends AuditModel {

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private int rating;

	@Column(nullable = false, length = 1000)
	private String body;

	/** "Tasnim R." - snapshot at writing time; never the phone or full surname. */
	@Column(name = "author_name", nullable = false, length = 40)
	private String authorName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ReviewStatus status = ReviewStatus.PUBLISHED;
}
