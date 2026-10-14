package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Product;
import com.taja.crm.crm_backend.repo.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final CurrencyService currencies;

    public Page<Product> findAllProducts(Pageable pageable) { return findAllProducts(pageable, null, null); }
    public Page<Product> findAllProducts(Pageable pageable, String currency, String status) {
        if (currency != null) currencies.require(currency, false);
        if (status != null && !java.util.Set.of("啟用", "停用").contains(status))
            throw new IllegalArgumentException("產品狀態必須為啟用或停用");
        return products.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (currency != null) predicates.add(cb.equal(root.get("currency"), currency));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id")));
    }
    public Product findByIdProduct(String id) {
        return products.findById(id).orElseThrow(() -> new EntityNotFoundException("找不到 Product：" + id));
    }

    @Transactional
    public Product createProducts(@NotNull @Valid Product product) {
        if (product.getId() != null) throw new IllegalArgumentException("新增產品不可指定 id");
        normalize(product);
        currencies.require(product.getCurrency(), true);
        if (products.existsBySkuIgnoreCase(product.getSku())) throw duplicate();
        return products.saveAndFlush(product);
    }

    @Transactional
    public Product updateProducts(String id, @NotNull @Valid Product product) {
        Product existing = findByIdProduct(id);
        normalize(product);
        if (products.existsBySkuIgnoreCaseAndIdNot(product.getSku(), id)) throw duplicate();
        existing.setName(product.getName()); existing.setSku(product.getSku());
        existing.setPrice(product.getPrice()); existing.setStatus(product.getStatus());
        return products.saveAndFlush(existing);
    }

    @Transactional
    public void deleteProducts(String id) {
        Product product = findByIdProduct(id);
        try {
            products.delete(product);
            products.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "產品已被引用，請改用停用。", exception);
        }
    }

    private void normalize(Product product) {
        product.setName(product.getName().strip()); product.setSku(product.getSku().strip());
        if (product.getName().isEmpty() || product.getSku().isEmpty())
            throw new IllegalArgumentException("名稱與產品編號不可為空白");
    }
    private ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "產品編號已存在。");
    }
}
