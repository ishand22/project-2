package com.entitleflow;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

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
    void record(String entityId,String type,String severity,String message,Map<String,Object> context){
        try { DiagnosticEvent event=new DiagnosticEvent(); event.entityId=entityId; event.eventType=type; event.severity=severity; event.message=message; event.context=context; events.save(event); }
        catch (RuntimeException ignored) { /* Mongo is a best-effort event sink; PostgreSQL history remains authoritative. */ }
    }
}
