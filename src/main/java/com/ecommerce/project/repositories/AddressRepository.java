package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Addresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Addresses,Long> {

}
