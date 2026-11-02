package com.ecommerce.user.repository;

import com.ecommerce.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

	/** Scoped lookup: another user's address is simply not found (404, not 403). */
	Optional<Address> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	@Modifying
	@Query("update Address a set a.defaultAddress = false where a.userId = :userId and a.id <> :keepId")
	int clearDefaultExcept(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
