package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

	/** Same rule as ProductRepository.takeStock, for one option of a product. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ProductOption o set o.stock = o.stock - :qty where o.id = :id and o.product.id = :productId and o.stock >= :qty")
	int takeStock(@Param("productId") Long productId, @Param("id") Long id, @Param("qty") int qty);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ProductOption o set o.stock = o.stock + :qty where o.id = :id and o.product.id = :productId")
	int returnStock(@Param("productId") Long productId, @Param("id") Long id, @Param("qty") int qty);
}
