package com.taja.crm.crm_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @NotBlank @Column(nullable = false)
    private String name;
    @NotBlank @Column(nullable = false)
    private String sku;
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @NotBlank @Pattern(regexp = "啟用|停用") @Column(nullable = false)
    private String status = "啟用";
}
