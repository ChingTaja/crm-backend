package com.taja.crm.crm_backend.dto.product;

import com.taja.crm.crm_backend.model.Product;
import java.math.BigDecimal;

public record ProductResponse(String id, String name, String sku, BigDecimal price, String status) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getPrice(), product.getStatus());
    }
}
