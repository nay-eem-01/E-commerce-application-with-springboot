package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	List<CartItem> findByUserIdOrderByIdAsc(Long userId);

	Optional<CartItem> findByIdAndUserId(Long id, Long userId);

	@Query("""
			select c from CartItem c where c.userId = :userId and c.productId = :productId
			and ((:optionId is null and c.optionId is null) or c.optionId = :optionId)
			""")
	Optional<CartItem> findLine(@Param("userId") Long userId, @Param("productId") Long productId, @Param("optionId") Long optionId);

	long countByUserId(Long userId);

	@Modifying
	@Query("delete from CartItem c where c.userId = :userId")
	int deleteByUserId(@Param("userId") Long userId);
}
