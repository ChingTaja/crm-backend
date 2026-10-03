package com.taja.crm.crm_backend.dto.order;
import com.taja.crm.crm_backend.model.*;
import java.time.Instant;
import java.util.List;
public record OrderResponse(String id,String number,String name,String status,long revision,String customerId,
 String customerName,String opportunityId,QuoteSource quoteSource,String currency,List<OrderLineResponse> lines,
 QuoteTotals totals,String paymentTerms,String deliveryTerms,String warranty,String notes,Instant createdAt,
 String createdBy,Instant updatedAt,Instant processingAt,Instant completedAt,Instant cancelledAt,
 String cancellationReason,List<OrderAudit> audit,List<OrderStatus> allowedTransitions) {
 public record QuoteSource(String quoteId,String quoteNumber,String quoteVersionId,int quoteVersion) {}
 public static OrderResponse of(SalesOrder o,List<OrderStatus> allowed) {
 var t=o.getTerms();
 return new OrderResponse(o.getId(),o.getNumber(),t.getName(),o.getStatus(),o.getRevision(),t.getCustomer().getId(),
 o.getCustomerName(),t.getOpportunity()==null?null:t.getOpportunity().getId(),
 new QuoteSource(o.getQuote().getId(),o.getQuoteNumber(),o.getQuoteVersionId(),o.getQuoteVersion()),
 o.getCurrency(),o.getLines().stream().map(OrderLineResponse::of).toList(),o.getTotals(),t.getPaymentTerms(),
 t.getDeliveryTerms(),t.getWarranty(),t.getNotes(),o.getCreatedAt(),o.getCreatedBy(),o.getUpdatedAt(),
 o.getProcessingAt(),o.getCompletedAt(),o.getCancelledAt(),o.getCancellationReason(),List.copyOf(o.getAudit()),allowed);
 }
}
