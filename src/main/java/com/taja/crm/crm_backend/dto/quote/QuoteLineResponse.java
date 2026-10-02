package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.QuoteLine;
import java.math.BigDecimal;
public record QuoteLineResponse(String id, String productId, String productName, String sku, BigDecimal catalogPrice,
        BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal taxPercent) {
    public static QuoteLineResponse of(QuoteLine l) {
        return new QuoteLineResponse(l.getId(), l.getProduct().getId(), l.getProductName(), l.getSku(), l.getCatalogPrice(),
                l.getQuantity(), l.getUnitPrice(), l.getDiscountPercent(), l.getTaxPercent());
    }
}
