package com.entitleflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EntitlementRepository extends JpaRepository<Entitlement,Long> {
    List<Entitlement> findByCustomerId(Long customerId);
    List<Entitlement> findByOrderId(Long orderId);
    List<Entitlement> findByStatus(EntitlementStatus status);
}
