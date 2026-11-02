package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

	List<WishlistItem> findByUserIdOrderByIdDesc(Long userId);

	Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

	long countByUserId(Long userId);
}
