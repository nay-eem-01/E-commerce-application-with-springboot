package com.ecommerce.user.repository;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(RoleType name);
}
