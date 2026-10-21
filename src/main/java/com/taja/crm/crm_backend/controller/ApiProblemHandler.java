package com.taja.crm.crm_backend.controller;
import com.taja.crm.crm_backend.service.QuoteException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class ApiProblemHandler {
 @ExceptionHandler(com.taja.crm.crm_backend.service.RateLimitExceeded.class)
 public ResponseEntity<ProblemDetail> rateLimited(com.taja.crm.crm_backend.service.RateLimitExceeded e) {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,e.getMessage());
  p.setProperty("code",com.taja.crm.crm_backend.service.RateLimitExceeded.CODE);
  return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After",Long.toString(e.getRetryAfterSeconds()))
   .header("Cache-Control","no-store").body(p);
 }
 @ExceptionHandler(QuoteException.class) public ProblemDetail denied(QuoteException e) {
  var p=ProblemDetail.forStatusAndDetail(e.getStatus(),e.getMessage());p.setProperty("code",e.getCode());return p;
 }
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,
  MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class})
 public ProblemDetail invalid(Exception e) {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"欄位格式不正確，請確認必填欄位與資料型別。");
  p.setProperty("code","VALIDATION_ERROR");return p;
 }
 @ExceptionHandler(DataIntegrityViolationException.class) public ProblemDetail conflict() {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"資料重複或仍被引用，請重新載入後再試。");
  p.setProperty("code","DATA_CONFLICT");return p;
 }
}
