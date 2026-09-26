package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.customer.CreateCustomerRequest;
import com.taja.crm.crm_backend.dto.customer.CustomerResponse;
import com.taja.crm.crm_backend.dto.customer.UpdateCustomerRequest;
import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.Pagination;
import org.springframework.web.bind.annotation.RequestParam;
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
    public PageResponse<CustomerResponse> findAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(customerService.findAllCustomers(Pagination.of(page, size)).map(CustomerResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public CustomerResponse findByIdCustomer(@PathVariable String id) {
        return CustomerResponse.fromEntity(customerService.findByIdCustomer(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomers(@Valid @RequestBody CreateCustomerRequest request) {
        Customer created = customerService.createCustomers(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.fromEntity(created));
    }

    /** 完整更新；未提供的欄位會清空。 */
    @PutMapping("/{id}")
    public CustomerResponse updateCustomers(@PathVariable String id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.fromEntity(customerService.updateCustomers(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomers(@PathVariable String id) {
        customerService.deleteCustomers(id);
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
