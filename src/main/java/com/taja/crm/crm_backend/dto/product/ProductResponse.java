package com.taja.crm.crm_backend.dto.product;

import com.taja.crm.crm_backend.model.Product;
import java.math.BigDecimal;

@io.swagger.v3.oas.annotations.media.Schema(requiredProperties = {"id", "name", "sku", "price", "currency", "status"})
public record ProductResponse(String id, String name, String sku, BigDecimal price, String currency, String status) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getPrice(), product.getCurrency(), product.getStatus());
    }
}
