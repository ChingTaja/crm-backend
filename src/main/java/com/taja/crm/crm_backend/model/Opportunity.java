package com.taja.crm.crm_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "opportunities")
@Getter
@Setter
public class Opportunity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @NotBlank @Column(nullable = false)
    private String name;
    private String owner;
    @NotBlank @Column(nullable = false)
    private String customerId;
    // 保留既有 Lead 轉換的聯絡人關聯，不對外列入商機 DTO。
    private String contactId;
    private String leadId;
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    private LocalDate expectedCloseDate;
    @NotBlank @Pattern(regexp = "需求確認|提案報價|協商中|已成交|已失單")
    @Column(nullable = false)
    private String stage = "需求確認";
}
