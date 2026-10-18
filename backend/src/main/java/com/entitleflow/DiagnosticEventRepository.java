package com.entitleflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface DiagnosticEventRepository extends MongoRepository<DiagnosticEvent,String> {
    List<DiagnosticEvent> findTop100ByOrderByTimestampDesc();
    List<DiagnosticEvent> findByEntityIdOrderByTimestampDesc(String entityId);
    boolean existsByEventTypeAndEntityIdAndSeverity(String eventType,String entityId,String severity);
    long countByEventTypeAndSeverity(String eventType,String severity);
    DiagnosticEvent findTopByEntityIdAndEventTypeInOrderByTimestampDesc(String entityId,List<String> eventTypes);
    long countByEventType(String eventType);
}
