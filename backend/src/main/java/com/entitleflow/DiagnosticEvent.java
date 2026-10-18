package com.entitleflow;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.List;

@Document("diagnostic_events")
class DiagnosticEvent {
    @Id String id;
    Instant timestamp=Instant.now();
    String entityType="ORDER", entityId, eventType, severity, message;
    Map<String,Object> context;
}
@Service
class EventRecorder {
    private final DiagnosticEventRepository events;
    EventRecorder(DiagnosticEventRepository events){this.events=events;}
    List<DiagnosticEvent> latest(){return events.findTop100ByOrderByTimestampDesc();}
    List<DiagnosticEvent> forEntity(String entityId){return events.findByEntityIdOrderByTimestampDesc(entityId);}
    long unresolvedFindings(){return events.countByEventType("RECONCILIATION_FINDING")-events.countByEventType("RECONCILIATION_RESOLVED");}
    boolean findingExists(String entityId){return events.existsByEventTypeAndEntityIdAndSeverity("RECONCILIATION_FINDING",entityId,"WARN");}
    DiagnosticEvent latestReconciliationEvent(String entityId){return events.findTopByEntityIdAndEventTypeInOrderByTimestampDesc(entityId,List.of("RECONCILIATION_FINDING","RECONCILIATION_RESOLVED"));}
    void record(String entityId,String type,String severity,String message,Map<String,Object> context){
        try { DiagnosticEvent event=new DiagnosticEvent(); event.entityId=entityId; event.eventType=type; event.severity=severity; event.message=message; event.context=context; events.save(event); }
        catch (RuntimeException ignored) { /* Mongo is a best-effort event sink; PostgreSQL history remains authoritative. */ }
    }
    void recordAsset(String entityId,String type,String severity,String message,Map<String,Object> context){
        try { DiagnosticEvent event=new DiagnosticEvent(); event.entityType="ASSET";event.entityId=entityId;event.eventType=type;event.severity=severity;event.message=message;event.context=context;events.save(event); }
        catch (RuntimeException ignored) { /* PostgreSQL asset state remains authoritative. */ }
    }
}
