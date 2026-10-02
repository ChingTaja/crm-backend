package com.taja.crm.crm_backend.model;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
@Embeddable @Getter @Setter
public class QuoteTotals {
    private long subtotalCents;
    private long discountCents;
    private long taxCents;
    private long totalCents;
}
