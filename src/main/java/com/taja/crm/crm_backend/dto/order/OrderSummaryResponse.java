package com.taja.crm.crm_backend.dto.order;
import com.taja.crm.crm_backend.model.*;
import java.time.Instant;
public record OrderSummaryResponse(String id,String number,String name,String customerId,String customerName,
 String quoteNumber,int quoteVersion,int lineCount,long totalCents,String currency,String status,Instant createdAt) {
 public static OrderSummaryResponse of(SalesOrder o) {return new OrderSummaryResponse(o.getId(),o.getNumber(),
 o.getTerms().getName(),o.getTerms().getCustomer().getId(),o.getCustomerName(),o.getQuoteNumber(),o.getQuoteVersion(),
 o.getLines().size(),o.getTotals().getTotalCents(),o.getCurrency(),o.getStatus(),o.getCreatedAt());}
}
