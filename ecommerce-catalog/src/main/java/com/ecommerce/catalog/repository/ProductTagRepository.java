package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.ProductTag;
import com.ecommerce.catalog.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

	/**
	 * Tag groups and values with how many products carry each, for the filter sheet. An empty
	 * slug means "any" (a null bound inside a comparison reaches PostgreSQL untyped).
	 */
	@Query("""
			select t.group, t.value, count(distinct p.id), min(t.sortOrder)
			from ProductTag t join t.product p join p.category c left join p.character ch
			where p.status = :status
			  and (:category = '' or c.slug = :category)
			  and (:character = '' or ch.slug = :character)
			group by t.group, t.value
			order by t.group asc, min(t.sortOrder) asc, t.value asc
			""")
	List<Object[]> facetCounts(@Param("status") ProductStatus status, @Param("category") String category,
							   @Param("character") String character);
}
