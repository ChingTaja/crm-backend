package com.taja.crm.crm_backend.dto.product;

import com.taja.crm.crm_backend.model.Product;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductRequest {
    @NotBlank @Size(max = 255)
    private String name;
    @NotBlank @Size(max = 255)
    private String sku;
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2)
    private BigDecimal price;
    @NotBlank @Pattern(regexp = "啟用|停用")
    private String status = "啟用";

    @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED, example = "TWD")
    private String currency;

    public Product toEntity() {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(price);
        product.setCurrency(currency);
        product.setStatus(status);
        return product;
    }
}
