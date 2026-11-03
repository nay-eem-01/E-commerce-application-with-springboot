package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.ShopCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopCharacterRepository extends JpaRepository<ShopCharacter, Long> {

	List<ShopCharacter> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<ShopCharacter> findAllByOrderBySortOrderAscNameAsc();

	Optional<ShopCharacter> findBySlug(String slug);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);
}
