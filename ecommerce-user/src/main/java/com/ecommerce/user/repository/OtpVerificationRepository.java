package com.ecommerce.user.repository;

import com.ecommerce.user.entity.OtpVerification;
import com.ecommerce.user.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

	Optional<OtpVerification> findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByIdDesc(String phone, OtpPurpose purpose);

	/** A new code makes every older unused code for the number useless. */
	@Modifying
	@Query("update OtpVerification o set o.consumedAt = :now where o.phone = :phone and o.purpose = :purpose and o.consumedAt is null")
	int consumeAllOpen(@Param("phone") String phone, @Param("purpose") OtpPurpose purpose, @Param("now") Instant now);
}
