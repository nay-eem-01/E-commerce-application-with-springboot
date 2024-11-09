package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepositories extends JpaRepository<Cart,Long> {
    @Query("SELECT c FROM Cart c WHERE c.users.userEmail = ?1")
    Cart findByEmailId(String email);
    @Query("SELECT c FROM Cart c WHERE c.users.userEmail = ?1 AND c.cartId = ?2")
    Cart findCartByEmailAndCartId(String emailId, Long cartId);
}
