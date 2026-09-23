package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.customer.CreateCustomerRequest;
import com.taja.crm.crm_backend.dto.customer.CustomerResponse;
import com.taja.crm.crm_backend.dto.customer.UpdateCustomerRequest;
import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public List<CustomerResponse> findAll() {
        return customerService.findAll().stream().map(CustomerResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable String id) {
        return CustomerResponse.fromEntity(customerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer created = customerService.create(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.fromEntity(created));
    }

    /** 完整更新；未提供的欄位會清空。 */
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable String id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.fromEntity(customerService.update(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
