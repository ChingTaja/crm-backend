package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.order.*;
import java.time.*;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class OrderService {
 private final SalesOrderRepository orders;
 private final PermissionService access;
 private final UserRepository users;
 private final Clock clock;
 private static final ZoneId ZONE=ZoneId.of("Asia/Taipei");
 private QuoteException error(HttpStatus status,String code,String detail) {return new QuoteException(status,code,detail);}
 private User actor(String id) {return users.findById(id).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","請重新登入。"));}
 private boolean manager(User u) {return u.getRole()!=null && Set.of("ADMIN","MANAGER").contains(u.getRole().getCode());}
 private boolean permitted(SalesOrder o,User u) {
  return manager(u)||o.getCreatedBy().equals(u.getId())||o.getQuote().getCreatedBy().equals(u.getId());
 }
 private SalesOrder load(String id,User actor,boolean lock) {
  var order=(lock?orders.findForUpdateById(id):orders.findById(id))
    .orElseThrow(()->error(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","找不到訂單。"));
  if(!permitted(order,actor)) throw error(HttpStatus.FORBIDDEN,"ORDER_FORBIDDEN","您沒有操作此訂單的權限。");
  return order;
 }
 private List<OrderStatus> transitions(SalesOrder o,String actorId) {
  List<OrderStatus> possible = switch(OrderStatus.valueOf(o.getStatus())) {
   case Confirmed -> List.of(OrderStatus.Processing,OrderStatus.Cancelled);
   case Processing -> List.of(OrderStatus.Completed,OrderStatus.Cancelled);
   default -> List.of();
  };
  var codes=access.permissions(actorId);
  return possible.stream().filter(status->codes.contains(permission(status))).toList();
 }
 private String permission(OrderStatus status) {
  return "orders."+switch(status) {case Processing -> "process";case Completed -> "complete";case Cancelled -> "cancel";default -> "unsupported";};
 }
 public OrderResponse findByIdOrder(String actorId,String id) {
  var o=load(id,actor(actorId),false);return OrderResponse.of(o,transitions(o,actorId));
 }
 public PageResponse<OrderSummaryResponse> findAllOrders(String actorId,int page,int size,String keyword,
  OrderStatus status,String customerId,LocalDate from,LocalDate to,String sort,String direction) {
  Pagination.of(page,size);
  if(from!=null&&to!=null&&from.isAfter(to)) throw new IllegalArgumentException("開始日期不可晚於結束日期。");
  Map<String,String> fields=Map.of("id","id","number","number","name","terms.name","customerName","customerName",
   "totalCents","totals.totalCents","status","status","createdAt","createdAt","quoteNumber","quoteNumber");
  if(!fields.containsKey(sort)||!Set.of("asc","desc").contains(direction))
   throw new IllegalArgumentException("不支援的排序欄位或方向。");
  User actor=actor(actorId);
  Specification<SalesOrder> spec=(root,query,cb)-> {
   List<Predicate> predicates=new ArrayList<>();
   if(!manager(actor)) predicates.add(cb.or(cb.equal(root.get("createdBy"),actorId),
      cb.equal(root.get("quote").get("createdBy"),actorId)));
   if(status!=null) predicates.add(cb.equal(root.get("status"),status.name()));
   if(customerId!=null&&!customerId.isBlank()) predicates.add(cb.equal(root.get("terms").get("customer").get("id"),customerId));
   if(from!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from.atStartOfDay(ZONE).toInstant()));
   if(to!=null) predicates.add(cb.lessThan(root.get("createdAt"),to.plusDays(1).atStartOfDay(ZONE).toInstant()));
   if(keyword!=null&&!keyword.isBlank()) {
    String like="%"+keyword.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
    predicates.add(cb.or(cb.like(cb.lower(root.get("number")),like,'!'),
      cb.like(cb.lower(root.get("terms").get("name")),like,'!'),
      cb.like(cb.lower(root.get("customerName")),like,'!'),cb.like(cb.lower(root.get("quoteNumber")),like,'!')));
   }
   return cb.and(predicates.toArray(Predicate[]::new));
  };
  Sort order=Sort.by(Sort.Direction.fromString(direction),fields.get(sort));
  if(!"id".equals(sort)) order=order.and(Sort.by("id"));
  return PageResponse.fromPage(orders.findAll(spec,PageRequest.of(page,size,order)).map(OrderSummaryResponse::of));
 }
 @Transactional
 public OrderResponse updateOrderStatus(String actorId,String id,UpdateOrderStatusRequest request) {
  access.require(actorId,permission(request.status()));
  User actor=actor(actorId);SalesOrder order=load(id,actor,true);
  if(order.getRevision()!=request.expectedRevision())
   throw error(HttpStatus.CONFLICT,"ORDER_REVISION_CONFLICT","訂單已被其他使用者更新，請重新載入。");
  if(!transitions(order,actorId).contains(request.status()))
   throw error(HttpStatus.CONFLICT,"ORDER_INVALID_TRANSITION","訂單目前狀態不允許此變更，已完成或取消的訂單不可再異動。");
  if(request.status()==OrderStatus.Cancelled&&(request.reason()==null||request.reason().isBlank()))
   throw error(HttpStatus.BAD_REQUEST,"ORDER_REASON_REQUIRED","取消訂單時必須填寫原因。");
  Instant now=clock.instant();
  OrderAudit audit=new OrderAudit();audit.setAt(now);audit.setActorId(actorId);audit.setActorName(actor.getUsername());
  audit.setAction("StatusChanged");audit.setFromStatus(order.getStatus());audit.setToStatus(request.status().name());
  audit.setReason(request.reason()==null?null:request.reason().strip());order.getAudit().add(audit);
  order.setStatus(request.status().name());order.setUpdatedAt(now);order.setRevision(order.getRevision()+1);
  switch(request.status()) {
   case Processing -> order.setProcessingAt(now);
   case Completed -> order.setCompletedAt(now);
   case Cancelled -> {order.setCancelledAt(now);order.setCancellationReason(request.reason().strip());}
   default -> {}
  }
  orders.flush();return OrderResponse.of(order,transitions(order,actorId));
 }
}
