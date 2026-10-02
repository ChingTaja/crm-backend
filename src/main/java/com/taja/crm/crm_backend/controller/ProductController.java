package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.product.CreateProductRequest;
import com.taja.crm.crm_backend.dto.product.ProductResponse;
import com.taja.crm.crm_backend.dto.product.UpdateProductRequest;
import com.taja.crm.crm_backend.model.Product;
import com.taja.crm.crm_backend.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> findAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(productService.findAllProducts(Pagination.of(page, size)).map(ProductResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ProductResponse findByIdProduct(@PathVariable String id) {
        return ProductResponse.fromEntity(productService.findByIdProduct(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProducts(@Valid @RequestBody CreateProductRequest request) {
        Product created = productService.createProducts(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(created));
    }

    /** 完整更新；未提供的欄位會清空。 */
    @PutMapping("/{id}")
    public ProductResponse updateProducts(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.fromEntity(productService.updateProducts(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducts(@PathVariable String id) {
        productService.deleteProducts(id);
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
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateSku() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "產品編號已存在。");
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail handleStatus(org.springframework.web.server.ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
}
