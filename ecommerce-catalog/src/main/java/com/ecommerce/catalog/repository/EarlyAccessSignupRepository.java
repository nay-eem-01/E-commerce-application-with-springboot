package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.entity.EarlyAccessSignup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarlyAccessSignupRepository extends JpaRepository<EarlyAccessSignup, Long> {

	boolean existsByCategoryIdAndPhone(Long categoryId, String phone);

	boolean existsByCategoryIdAndEmailIgnoreCase(Long categoryId, String email);

	@EntityGraph(attributePaths = {"category"})
	Page<EarlyAccessSignup> findByCategoryId(Long categoryId, Pageable pageable);

	@EntityGraph(attributePaths = {"category"})
	Page<EarlyAccessSignup> findAllBy(Pageable pageable);
}
