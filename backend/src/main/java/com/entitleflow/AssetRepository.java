package com.entitleflow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
public interface AssetRepository extends JpaRepository<Asset,Long> {
    List<Asset> findByStatus(AssetStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.product=:product and a.status=:status order by a.id")
    List<Asset> lockAvailable(@Param("product") String product,@Param("status") AssetStatus status,Pageable pageable);
}
