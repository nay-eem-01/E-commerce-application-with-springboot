package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepositories extends JpaRepository<Order,Long> {
}
