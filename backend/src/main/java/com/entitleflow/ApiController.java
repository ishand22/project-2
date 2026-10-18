package com.entitleflow;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
@Transactional
class ApiController {
    private final CustomerRepository customers; private final AssetRepository assets; private final OrderRepository orders;
    private final EntitlementRepository entitlements; private final OrderService orderService; private final EventRecorder events;
    ApiController(CustomerRepository c,AssetRepository a,OrderRepository o,EntitlementRepository e,OrderService s,EventRecorder events){customers=c;assets=a;orders=o;entitlements=e;orderService=s;this.events=events;}
    @GetMapping("/customers") List<Map<String,Object>> customers(@RequestParam(defaultValue="") String q){return customers.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q,q,PageRequest.of(0,100)).getContent().stream().map(this::customerDto).toList();}
    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createCustomer(@Valid @RequestBody CustomerRequest r){return customerDto(customers.save(new Customer(r.name().trim(),r.email().trim().toLowerCase(Locale.ROOT),r.company())));}
    @GetMapping("/customers/{id}") Map<String,Object> customer(@PathVariable Long id){return customerDto(customerOr404(id));}
    @PutMapping("/customers/{id}") Map<String,Object> updateCustomer(@PathVariable Long id,@Valid @RequestBody CustomerRequest r){Customer c=customerOr404(id);c.name=r.name().trim();c.email=r.email().trim().toLowerCase(Locale.ROOT);c.company=r.company();return customerDto(customers.save(c));}
    @GetMapping("/assets") List<Map<String,Object>> assetList(@RequestParam(required=false) AssetStatus status,@RequestParam(defaultValue="") String q){return assets.findAll().stream().filter(a->status==null||a.status==status).filter(a->a.product.toLowerCase().contains(q.toLowerCase())||a.model.toLowerCase().contains(q.toLowerCase())).map(this::assetDto).toList();}
    @PostMapping("/assets") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> registerAsset(@Valid @RequestBody AssetRequest r){Asset a=new Asset();a.assetTag=r.assetTag();a.serialNumber=r.serialNumber();a.product=r.product();a.model=r.model();a.warrantyExpiry=r.warrantyExpiry();return assetDto(assets.save(a));}
    @GetMapping("/assets/{id}") Map<String,Object> asset(@PathVariable Long id){return assetDto(assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found")));}
    @GetMapping("/orders") List<Map<String,Object>> orderList(){return orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0,100)).getContent().stream().map(this::orderDto).toList();}
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> submit(@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody OrderRequest r){return orderDto(orderService.submit(r,key));}
    @GetMapping("/orders/{id}") Map<String,Object> order(@PathVariable Long id){return orderDto(orderOr404(id));}
    @PostMapping("/orders/{id}/retry") Map<String,Object> retry(@PathVariable Long id){return orderDto(orderService.retry(id));}
    @GetMapping("/entitlements") List<Map<String,Object>> entitlementList(@RequestParam(required=false) Long customerId){return (customerId==null?entitlements.findAll():entitlements.findByCustomerId(customerId)).stream().map(this::entitlementDto).toList();}
    @PostMapping("/entitlements/{id}/activate") @Transactional Map<String,Object> activate(@PathVariable Long id){Entitlement e=entitlements.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Entitlement not found"));if(e.status!=EntitlementStatus.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only pending entitlements can be activated");e.status=EntitlementStatus.ACTIVE;e.activatedAt=Instant.now();events.record(e.entitlementKey,"ENTITLEMENT_ACTIVATED","INFO","Entitlement activated",Map.of("order",e.order.orderNumber));return entitlementDto(entitlements.save(e));}
    @PostMapping("/entitlements/{id}/revoke") @Transactional Map<String,Object> revoke(@PathVariable Long id){Entitlement e=entitlements.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Entitlement not found"));if(e.status==EntitlementStatus.REVOKED||e.status==EntitlementStatus.EXPIRED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Entitlement cannot be revoked in its current state");e.status=EntitlementStatus.REVOKED;events.record(e.entitlementKey,"ENTITLEMENT_REVOKED","WARN","Entitlement revoked",Map.of());return entitlementDto(entitlements.save(e));}
    @GetMapping("/analytics/overview") Map<String,Object> analytics(){List<OrderRecord> all=orders.findAll();long total=all.size();long completed=all.stream().filter(o->o.status==OrderStatus.COMPLETED).count();long failed=all.stream().filter(o->o.status==OrderStatus.FAILED).count();return Map.of("customers",customers.count(),"assets",assets.count(),"orders",total,"completedOrders",completed,"activeEntitlements",entitlements.findByStatus(EntitlementStatus.ACTIVE).size(),"failedOrders",failed,"completionRate",total==0?0.0:completed*100.0/total,"recentOrders",orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0,8)).getContent().stream().map(this::orderDto).toList());}
    @GetMapping("/orders/{id}/evidence") Map<String,Object> evidence(@PathVariable Long id){OrderRecord o=orderOr404(id);return Map.of("order",orderDto(o),"entitlements",entitlements.findByOrderId(id).stream().map(this::entitlementDto).toList());}
    private Customer customerOr404(Long id){return customers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));}
    private OrderRecord orderOr404(Long id){return orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));}
    private Map<String,Object> customerDto(Customer c){return Map.of("id",c.id,"name",c.name,"email",c.email,"company",c.company==null?"":c.company,"createdAt",c.createdAt.toString());}
    private Map<String,Object> assetDto(Asset a){return Map.of("id",a.id,"assetTag",a.assetTag,"serialNumber",a.serialNumber,"product",a.product,"model",a.model,"status",a.status.name(),"customer",a.customer==null?"":a.customer.name,"warrantyExpiry",a.warrantyExpiry==null?"":a.warrantyExpiry.toString());}
    private Map<String,Object> orderDto(OrderRecord o){return Map.of("id",o.id,"orderNumber",o.orderNumber,"customerId",o.customer.id,"customer",o.customer.name,"status",o.status.name(),"failureReason",o.failureReason==null?"":o.failureReason,"retryCount",o.retryCount,"createdAt",o.createdAt.toString(),"items",o.items.stream().map(i->Map.of("product",i.product,"quantity",i.quantity,"licenseType",i.licenseType)).toList(),"transitions",o.transitions.stream().map(t->Map.of("from",t.fromState==null?"":t.fromState,"to",t.toState,"details",t.details==null?"":t.details,"createdAt",t.createdAt.toString())).toList());}
    private Map<String,Object> entitlementDto(Entitlement e){return Map.ofEntries(Map.entry("id",e.id),Map.entry("entitlementKey",e.entitlementKey),Map.entry("customerId",e.customer.id),Map.entry("customer",e.customer.name),Map.entry("orderId",e.order.id),Map.entry("orderNumber",e.order.orderNumber),Map.entry("product",e.product),Map.entry("licenseType",e.licenseType),Map.entry("quantity",e.quantity),Map.entry("status",e.status.name()),Map.entry("activatedAt",e.activatedAt==null?"":e.activatedAt.toString()));}
}
