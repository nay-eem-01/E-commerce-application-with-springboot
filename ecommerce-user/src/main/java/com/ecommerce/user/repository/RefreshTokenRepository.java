package com.ecommerce.user.repository;

import com.ecommerce.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("delete from RefreshToken t where t.sessionId = :sessionId")
	int deleteBySessionId(@Param("sessionId") Long sessionId);

	@Modifying
	@Query("delete from RefreshToken t where t.userId = :userId")
	int deleteByUserId(@Param("userId") Long userId);
}
