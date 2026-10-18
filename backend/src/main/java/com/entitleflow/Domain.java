package com.entitleflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

enum OrderStatus { CREATED, VALIDATED, ASSETS_RESERVED, ENTITLEMENTS_CREATED, COMPLETED, FAILED, CANCELLED }
enum AssetStatus { AVAILABLE, RESERVED, ALLOCATED, IN_SERVICE, RETIRED }
enum EntitlementStatus { PENDING, ACTIVE, EXPIRED, REVOKED }

@Entity @Table(name="customers")
class Customer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=160) String name;
    @Column(nullable=false,unique=true,length=254) String email;
    @Column(length=160) String company;
    @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    protected Customer() {}
    Customer(String name,String email,String company){this.name=name;this.email=email;this.company=company;}
}
@Entity @Table(name="assets")
class Asset {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="asset_tag",nullable=false,unique=true) String assetTag;
    @Column(name="serial_number",nullable=false,unique=true) String serialNumber;
    @Column(nullable=false) String product; @Column(nullable=false) String model;
    @Enumerated(EnumType.STRING) @Column(nullable=false) AssetStatus status=AssetStatus.AVAILABLE;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id") Customer customer;
    @Column(name="warranty_expiry") LocalDate warrantyExpiry;
    protected Asset() {}
}
@Entity @Table(name="orders")
class OrderRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="order_number",nullable=false,unique=true) String orderNumber;
    @Column(name="idempotency_key",nullable=false,unique=true) String idempotencyKey;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id",nullable=false) Customer customer;
    @Enumerated(EnumType.STRING) @Column(nullable=false) OrderStatus status=OrderStatus.CREATED;
    @Column(name="failure_reason") String failureReason;
    @Column(name="retry_count",nullable=false) int retryCount;
    @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) Instant updatedAt=Instant.now();
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) List<OrderItem> items=new ArrayList<>();
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @OrderBy("createdAt ASC") List<OrderTransition> transitions=new ArrayList<>();
    protected OrderRecord() {}
}
@Entity @Table(name="order_items")
class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) OrderRecord order;
    @Column(nullable=false) String product; @Column(nullable=false) int quantity;
    @Column(name="license_type",nullable=false) String licenseType;
    protected OrderItem() {}
}
@Entity @Table(name="order_transitions")
class OrderTransition {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) OrderRecord order;
    @Column(name="from_state") String fromState; @Column(name="to_state",nullable=false) String toState;
    String details; @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    protected OrderTransition() {}
    OrderTransition(OrderRecord order,String from,String to,String details){this.order=order;this.fromState=from;this.toState=to;this.details=details;}
}
@Entity @Table(name="entitlements")
class Entitlement {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="entitlement_key",nullable=false,unique=true) String entitlementKey;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id",nullable=false) Customer customer;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) OrderRecord order;
    @Column(nullable=false) String product; @Column(name="license_type",nullable=false) String licenseType;
    @Column(nullable=false) int quantity; @Enumerated(EnumType.STRING) @Column(nullable=false) EntitlementStatus status;
    @Column(name="activated_at") Instant activatedAt; @Column(name="expires_at") Instant expiresAt;
    protected Entitlement() {}
}
