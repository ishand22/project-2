package com.entitleflow;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@Service
class EntitlementExpiryService {
    private final EntitlementRepository entitlements; private final EventRecorder events;
    EntitlementExpiryService(EntitlementRepository entitlements,EventRecorder events){this.entitlements=entitlements;this.events=events;}
    @Scheduled(cron="0 15 1 * * *",zone="UTC")
    @Transactional
    public void expireDueEntitlements(){
        for(Entitlement e:entitlements.findByStatusAndExpiresAtBefore(EntitlementStatus.ACTIVE,Instant.now())){
            e.status=EntitlementStatus.EXPIRED;entitlements.save(e);
            events.record(e.entitlementKey,"ENTITLEMENT_EXPIRED","WARN","Entitlement reached its expiry date",Map.of("order",e.order.orderNumber,"expiresAt",e.expiresAt.toString()));
        }
    }
}

@Service
class ReconciliationService {
    private final OrderRepository orders; private final EntitlementRepository entitlements; private final EventRecorder events;
    ReconciliationService(OrderRepository orders,EntitlementRepository entitlements,EventRecorder events){this.orders=orders;this.entitlements=entitlements;this.events=events;}
    @Scheduled(fixedDelay=21600000,initialDelay=60000)
    @Transactional
    public void scheduledReconciliation(){reconcile();}
    @Transactional
    public int reconcile(){
        int findings=0;
        for(OrderRecord order:orders.findAll()){
            if(order.status!=OrderStatus.COMPLETED)continue;
            int expected=order.items.stream().mapToInt(i->i.quantity).sum();
            int actual=entitlements.findByOrderId(order.id).stream().mapToInt(e->e.quantity).sum();
            DiagnosticEvent latest=events.latestReconciliationEvent(order.orderNumber);
            if(actual<expected&&(latest==null||!"RECONCILIATION_FINDING".equals(latest.eventType))){
                events.record(order.orderNumber,"RECONCILIATION_FINDING","WARN","Completed order has fewer entitlement units than requested",Map.of("expectedUnits",expected,"actualUnits",actual));
                findings++;
            } else if(actual>=expected&&latest!=null&&"RECONCILIATION_FINDING".equals(latest.eventType)){
                events.record(order.orderNumber,"RECONCILIATION_RESOLVED","INFO","Previously detected order inconsistency is no longer present",Map.of("expectedUnits",expected,"actualUnits",actual));
            }
        }
        return findings;
    }
}
