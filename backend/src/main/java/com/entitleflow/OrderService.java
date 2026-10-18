package com.entitleflow;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
class OrderService {
    private final OrderRepository orders; private final CustomerRepository customers; private final AssetRepository assets;
    private final EntitlementRepository entitlements; private final EventRecorder events;
    OrderService(OrderRepository orders,CustomerRepository customers,AssetRepository assets,EntitlementRepository entitlements,EventRecorder events){this.orders=orders;this.customers=customers;this.assets=assets;this.entitlements=entitlements;this.events=events;}

    @Transactional
    OrderRecord submit(OrderRequest request,String key) {
        if(key==null || key.isBlank() || key.length()>160) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid Idempotency-Key header is required");
        Optional<OrderRecord> prior=orders.findByIdempotencyKey(key);
        if(prior.isPresent()) { if(!samePayload(prior.get(),request)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key was already used for a different request"); return prior.get(); }
        Customer customer=customers.findById(request.customerId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));
        if(request.items().stream().map(OrderItemRequest::product).distinct().count()!=request.items().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use one line per product");
        OrderRecord order=new OrderRecord(); order.customer=customer; order.idempotencyKey=key; order.orderNumber="ORD-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        request.items().forEach(i->{OrderItem line=new OrderItem();line.order=order;line.product=i.product().trim();line.quantity=i.quantity();line.licenseType=i.licenseType().trim();order.items.add(line);});
        orders.saveAndFlush(order); transition(order,OrderStatus.CREATED,"Order submitted");
        List<Asset> reserved=new ArrayList<>();
        for(OrderItem item:order.items){
            List<Asset> found=assets.lockAvailable(item.product,AssetStatus.AVAILABLE,PageRequest.of(0,item.quantity));
            if(found.size()<item.quantity){order.status=OrderStatus.FAILED;order.failureReason="Insufficient available inventory for "+item.product+": requested "+item.quantity+", found "+found.size();transition(order,OrderStatus.FAILED,order.failureReason);order.updatedAt=Instant.now();orders.save(order);events.record(order.orderNumber,"ORDER_FAILED","ERROR",order.failureReason,Map.of("product",item.product));return order;}
            reserved.addAll(found);
        }
        transition(order,OrderStatus.VALIDATED,"Customer and requested products validated");
        reserved.forEach(asset->{asset.status=AssetStatus.ALLOCATED;asset.customer=customer;assets.save(asset);events.recordAsset(asset.assetTag,"ASSET_ALLOCATED","INFO","Asset allocated to order",Map.of("order",order.orderNumber,"customerId",customer.id));});
        transition(order,OrderStatus.ASSETS_RESERVED,"Reserved "+reserved.size()+" matching assets");
        for(OrderItem item:order.items){Entitlement entitlement=new Entitlement();entitlement.entitlementKey="ENT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);entitlement.customer=customer;entitlement.order=order;entitlement.product=item.product;entitlement.licenseType=item.licenseType;entitlement.quantity=item.quantity;entitlement.status=EntitlementStatus.PENDING;if("SUBSCRIPTION".equalsIgnoreCase(item.licenseType))entitlement.expiresAt=Instant.now().plusSeconds(31536000);entitlements.save(entitlement);}
        transition(order,OrderStatus.ENTITLEMENTS_CREATED,"Pending entitlements created");
        transition(order,OrderStatus.COMPLETED,"Order fulfilled"); order.updatedAt=Instant.now(); orders.save(order);
        events.record(order.orderNumber,"ORDER_COMPLETED","INFO","Order completed",Map.of("assetsAllocated",reserved.size(),"entitlementsCreated",order.items.size()));
        return order;
    }
    @Transactional
    OrderRecord retry(Long id){OrderRecord order=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
        if(order.status!=OrderStatus.FAILED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only failed orders can be retried");
        if(order.retryCount>=3) throw new ResponseStatusException(HttpStatus.CONFLICT,"Retry limit reached");
        order.retryCount++;order.failureReason=null;transition(order,OrderStatus.CREATED,"Retry attempt "+order.retryCount);orders.save(order);
        // Keep the original idempotency key and existing order: continue the same row without duplicating effects.
        List<Asset> reserved=new ArrayList<>();
        for(OrderItem item:order.items){List<Asset> found=assets.lockAvailable(item.product,AssetStatus.AVAILABLE,PageRequest.of(0,item.quantity));if(found.size()<item.quantity){order.status=OrderStatus.FAILED;order.failureReason="Insufficient available inventory for "+item.product;transition(order,OrderStatus.FAILED,order.failureReason);return order;}reserved.addAll(found);}
        transition(order,OrderStatus.VALIDATED,"Retry validation passed");reserved.forEach(a->{a.status=AssetStatus.ALLOCATED;a.customer=order.customer;assets.save(a);events.recordAsset(a.assetTag,"ASSET_ALLOCATED","INFO","Asset allocated to order retry",Map.of("order",order.orderNumber));});transition(order,OrderStatus.ASSETS_RESERVED,"Retry reserved "+reserved.size()+" assets");
        if(entitlements.findByOrderId(order.id).isEmpty())for(OrderItem item:order.items){Entitlement e=new Entitlement();e.entitlementKey="ENT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);e.customer=order.customer;e.order=order;e.product=item.product;e.licenseType=item.licenseType;e.quantity=item.quantity;e.status=EntitlementStatus.PENDING;if("SUBSCRIPTION".equalsIgnoreCase(item.licenseType))e.expiresAt=Instant.now().plusSeconds(31536000);entitlements.save(e);}
        transition(order,OrderStatus.ENTITLEMENTS_CREATED,"Retry entitlements ready");transition(order,OrderStatus.COMPLETED,"Retry completed");order.updatedAt=Instant.now();events.record(order.orderNumber,"ORDER_RETRIED","INFO","Order retry completed",Map.of("attempt",order.retryCount));return order;
    }
    private boolean samePayload(OrderRecord order,OrderRequest request){return Objects.equals(order.customer.id,request.customerId())&&order.items.size()==request.items().size()&&order.items.stream().allMatch(i->request.items().stream().anyMatch(r->i.product.equals(r.product().trim())&&i.quantity==r.quantity()&&i.licenseType.equals(r.licenseType().trim())));}
    private void transition(OrderRecord o,OrderStatus next,String details){OrderStatus previous=o.status; o.status=next;o.updatedAt=Instant.now();o.transitions.add(new OrderTransition(o,previous==null?null:previous.name(),next.name(),details));events.record(o.orderNumber,"ORDER_STATE_CHANGED","INFO",details,Map.of("from",previous==null?"":previous.name(),"to",next.name()));}
}
