package com.entitleflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;
public interface EntitlementRepository extends JpaRepository<Entitlement,Long> {
    List<Entitlement> findByCustomerId(Long customerId);
    List<Entitlement> findByOrderId(Long orderId);
    List<Entitlement> findByStatus(EntitlementStatus status);
    List<Entitlement> findByStatusAndExpiresAtBefore(EntitlementStatus status, Instant instant);
}
