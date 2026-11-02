package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = {"items"})
	Optional<Order> findByOrderNumber(String orderNumber);

	/** Scoped: someone else's order number is simply not found. */
	@EntityGraph(attributePaths = {"items"})
	Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

	@EntityGraph(attributePaths = {"items"})
	Optional<Order> findByOrderNumberAndPhone(String orderNumber, String phone);

	Optional<Order> findByIdempotencyKey(String idempotencyKey);

	boolean existsByOrderNumber(String orderNumber);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	@Query("""
			select o from Order o
			where (:status is null or o.status = :status)
			and (:q = '' or o.orderNumber = :q or o.phone like concat('%', :q, '%'))
			""")
	Page<Order> search(@Param("status") OrderStatus status, @Param("q") String q, Pageable pageable);

	@Query("""
			select count(i) > 0 from OrderItem i
			where i.productId = :productId and i.order.userId = :userId
			and i.order.status = com.ecommerce.order.enums.OrderStatus.DELIVERED
			""")
	boolean hasReceived(@Param("userId") Long userId, @Param("productId") Long productId);
}
