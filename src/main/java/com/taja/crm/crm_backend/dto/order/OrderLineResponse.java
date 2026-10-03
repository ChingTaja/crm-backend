package com.taja.crm.crm_backend.dto.order;
import java.math.BigDecimal;
import com.taja.crm.crm_backend.model.QuoteLine;
public record OrderLineResponse(String id,String productId,String productName,String sku,BigDecimal catalogPrice,
 BigDecimal quantity,BigDecimal unitPrice,BigDecimal discountPercent,BigDecimal taxPercent,
 long subtotalCents,long discountCents,long taxCents,long totalCents) {
 public static OrderLineResponse of(QuoteLine l) { return new OrderLineResponse(l.getId(),l.getProduct().getId(),
 l.getProductName(),l.getSku(),l.getCatalogPrice(),l.getQuantity(),l.getUnitPrice(),l.getDiscountPercent(),
 l.getTaxPercent(),l.getSubtotalCents(),l.getDiscountCents(),l.getTaxCents(),l.getTotalCents()); }
}
