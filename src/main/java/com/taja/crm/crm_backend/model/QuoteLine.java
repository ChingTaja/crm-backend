package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Embeddable @Getter @Setter
public class QuoteLine {
    @Column(name = "line_id", nullable = false)
    private String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false) private String productName;
    @Column(nullable = false) private String sku;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal catalogPrice;
    private Long subtotalCents;
    private Long discountCents;
    private Long taxCents;
    private Long totalCents;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 7, scale = 4) private BigDecimal discountPercent;
    @Column(nullable = false, precision = 7, scale = 4) private BigDecimal taxPercent;
}
