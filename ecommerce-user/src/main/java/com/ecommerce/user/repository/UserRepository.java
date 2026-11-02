package com.ecommerce.user.repository;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByPhone(String phone);

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	@Query("select count(u) > 0 from User u join u.roles r where r.name = :role")
	boolean existsWithRole(@Param("role") RoleType role);

	@Query("""
			select u from User u
			where (:q = ''
				or lower(u.name) like concat('%', :q, '%')
				or u.phone like concat('%', :q, '%')
				or u.email like concat('%', :q, '%'))
			""")
	Page<User> search(@Param("q") String q, Pageable pageable);
}
