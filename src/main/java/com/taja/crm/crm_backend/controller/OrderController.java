package com.taja.crm.crm_backend.controller;
import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.order.*;
import com.taja.crm.crm_backend.model.OrderStatus;
import com.taja.crm.crm_backend.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
 private final OrderService service;
 @GetMapping public PageResponse<OrderSummaryResponse> findAllOrders(Principal actor,
  @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
  @RequestParam(required=false) String keyword,@RequestParam(required=false) OrderStatus status,
  @RequestParam(required=false) String customerId,@RequestParam(required=false) LocalDate createdFrom,
  @RequestParam(required=false) LocalDate createdTo,@RequestParam(defaultValue="createdAt") String sort,
  @RequestParam(defaultValue="desc") String direction) {
  return service.findAllOrders(actor.getName(),page,size,keyword,status,customerId,createdFrom,createdTo,sort,direction);
 }
 @GetMapping("/{id}") public OrderResponse findByIdOrder(Principal actor,@PathVariable String id) {
  return service.findByIdOrder(actor.getName(),id);
 }
 @PatchMapping("/{id}/status") public OrderResponse updateOrderStatus(Principal actor,@PathVariable String id,
   @Valid @RequestBody UpdateOrderStatusRequest request) {return service.updateOrderStatus(actor.getName(),id,request);}
 @ExceptionHandler(QuoteException.class) public ProblemDetail problem(QuoteException e) {
  var p=ProblemDetail.forStatusAndDetail(e.getStatus(),e.getMessage());p.setProperty("code",e.getCode());return p;
 }
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,
  MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class})
 public ProblemDetail invalid(Exception e) {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
   e instanceof IllegalArgumentException?e.getMessage():"訂單欄位格式不正確，請確認狀態、日期與版本。");
  p.setProperty("code","ORDER_VALIDATION_ERROR");return p;
 }
}
