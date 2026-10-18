package com.entitleflow;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name,String email,Pageable pageable);
}
