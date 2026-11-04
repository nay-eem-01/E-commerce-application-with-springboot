package com.ecommerce.review.repository;

import com.ecommerce.review.entity.Review;
import com.ecommerce.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	/** Moderation list; a null status or product means "any". */
	@Query("select r from Review r where (:status is null or r.status = :status) and (:productId is null or r.productId = :productId)")
	Page<Review> adminSearch(@Param("status") ReviewStatus status, @Param("productId") Long productId, Pageable pageable);

	Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

	boolean existsByProductIdAndUserId(Long productId, Long userId);

	Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

	@Query("select r.rating, count(r) from Review r where r.productId = :productId and r.status = :status group by r.rating")
	List<Object[]> ratingCounts(@Param("productId") Long productId, @Param("status") ReviewStatus status);
}
