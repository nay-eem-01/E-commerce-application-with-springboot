package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

	Optional<UserSession> findByPublicId(UUID publicId);

	@Modifying
	@Query("update UserSession s set s.active = false, s.revokedAt = :now where s.userId = :userId and s.active = true")
	int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
