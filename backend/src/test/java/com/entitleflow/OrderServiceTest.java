package com.entitleflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orders; @Mock CustomerRepository customers; @Mock AssetRepository assets;
    @Mock EntitlementRepository entitlements; @Mock EventRecorder events;

    @Test void sameIdempotencyKeyAndPayloadReturnsExistingOrder(){
        OrderService service=new OrderService(orders,customers,assets,entitlements,events);
        Customer customer=new Customer("Acme","ops@acme.test",null);customer.id=7L;
        OrderRecord existing=new OrderRecord();existing.id=12L;existing.orderNumber="ORD-123";existing.idempotencyKey="key-1";existing.customer=customer;
        OrderItem item=new OrderItem();item.product="Router X";item.quantity=1;item.licenseType="SUBSCRIPTION";existing.items.add(item);
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));
        OrderRecord actual=service.submit(new OrderRequest(7L,List.of(new OrderItemRequest("Router X",1,"SUBSCRIPTION"))),"key-1");
        assertSame(existing,actual);verify(orders,never()).saveAndFlush(any());verifyNoInteractions(assets,entitlements);
    }

    @Test void insufficientInventoryPersistsFailedOrderAndDoesNotCreateEntitlements(){
        OrderService service=new OrderService(orders,customers,assets,entitlements,events);
        Customer customer=new Customer("Acme","ops@acme.test",null);customer.id=7L;
        when(orders.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());when(customers.findById(7L)).thenReturn(Optional.of(customer));
        when(orders.saveAndFlush(any())).thenAnswer(call->call.getArgument(0));
        when(assets.lockAvailable(eq("Router X"),eq(AssetStatus.AVAILABLE),any(Pageable.class))).thenReturn(List.of());
        OrderRecord result=service.submit(new OrderRequest(7L,List.of(new OrderItemRequest("Router X",1,"SUBSCRIPTION"))),"key-2");
        assertEquals(OrderStatus.FAILED,result.status);assertTrue(result.failureReason.contains("Insufficient available inventory"));
        assertTrue(result.transitions.stream().anyMatch(t->"FAILED".equals(t.toState)));
        verify(entitlements,never()).save(any());
    }

    @Test void reusingKeyForDifferentPayloadIsRejected(){
        OrderService service=new OrderService(orders,customers,assets,entitlements,events);
        Customer customer=new Customer("Acme","ops@acme.test",null);customer.id=7L;
        OrderRecord existing=new OrderRecord();existing.customer=customer;existing.idempotencyKey="same";
        OrderItem item=new OrderItem();item.product="Router X";item.quantity=1;item.licenseType="SUBSCRIPTION";existing.items.add(item);
        when(orders.findByIdempotencyKey("same")).thenReturn(Optional.of(existing));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.submit(new OrderRequest(7L,List.of(new OrderItemRequest("Different product",1,"SUBSCRIPTION"))),"same"));
    }
}
