package com.ecommerce.project.repositories;

import com.ecommerce.project.model.APP_ROLE;
import com.ecommerce.project.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles, Long> {

    Optional<Roles> findByRoleName(APP_ROLE appRole);
}
