package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.enums.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@EntityGraph(attributePaths = {"category"})
	Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	boolean existsByCategoryId(Long categoryId);

	@EntityGraph(attributePaths = {"category"})
	List<Product> findByIdIn(Collection<Long> ids);

	@Query("select p.category.id, count(p) from Product p where p.status = :status group by p.category.id")
	List<Object[]> countByCategory(@Param("status") ProductStatus status);

	@EntityGraph(attributePaths = {"category"})
	List<Product> findTop8ByCategoryIdAndStatusAndIdNotOrderByCreationDateDesc(Long categoryId, ProductStatus status, Long id);

	@EntityGraph(attributePaths = {"category"})
	List<Product> findTop8ByStatusAndCategoryIdNotOrderByCreationDateDesc(ProductStatus status, Long categoryId);

	/**
	 * Takes stock only if enough is left; returns 0 when it is not. A conditional UPDATE is
	 * atomic in PostgreSQL, so two shoppers can never both buy the last unit.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Product p set p.stock = p.stock - :qty, p.version = p.version + 1
			where p.id = :id and p.stock >= :qty and p.status = com.ecommerce.catalog.enums.ProductStatus.ACTIVE
			""")
	int takeStock(@Param("id") Long id, @Param("qty") int qty);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Product p set p.stock = p.stock + :qty, p.version = p.version + 1 where p.id = :id")
	int returnStock(@Param("id") Long id, @Param("qty") int qty);
}
