package com.entitleflow;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<OrderRecord,Long> {
    Optional<OrderRecord> findByIdempotencyKey(String key);
    Page<OrderRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
