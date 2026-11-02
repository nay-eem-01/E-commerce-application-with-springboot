package com.ecommerce.payment.repository;

import com.ecommerce.payment.api.PaymentStatus;
import com.ecommerce.payment.entity.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

	List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

	Optional<Payment> findByReference(String reference);

	Optional<Payment> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

	@Query("select p from Payment p where p.status = :status and p.expiresAt is not null and p.expiresAt < :now order by p.expiresAt")
	List<Payment> findExpiring(@Param("status") PaymentStatus status, @Param("now") Instant now, Pageable pageable);
}
