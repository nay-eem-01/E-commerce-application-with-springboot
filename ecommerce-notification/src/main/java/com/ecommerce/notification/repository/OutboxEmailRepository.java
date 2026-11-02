package com.ecommerce.notification.repository;

import com.ecommerce.notification.entity.OutboxEmail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, Long> {

	boolean existsByDedupKey(String dedupKey);

	/**
	 * Locks due rows that no other instance has locked (SKIP LOCKED), so several app instances
	 * can dispatch at once without sending an email twice. Must run inside a transaction.
	 */
	@Query(value = """
			SELECT * FROM notification_outbox
			WHERE status = 'PENDING' AND next_attempt_at <= :now
			ORDER BY id
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEmail> lockDue(@Param("now") Instant now, @Param("limit") int limit);

	@Query("""
			select e from OutboxEmail e
			where (:status is null or e.status = :status)
			and (:q = '' or e.dedupKey like concat(:q, '%'))
			""")
	Page<OutboxEmail> search(@Param("status") OutboxEmail.Status status, @Param("q") String q, Pageable pageable);

	@Modifying
	@Query("delete from OutboxEmail e where e.status = com.ecommerce.notification.entity.OutboxEmail.Status.SENT and e.sentAt < :before")
	int deleteSentBefore(@Param("before") Instant before);
}
