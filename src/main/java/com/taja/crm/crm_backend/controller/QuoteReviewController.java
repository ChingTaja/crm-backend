package com.taja.crm.crm_backend.controller;
import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.quote.*;
import com.taja.crm.crm_backend.service.QuoteService;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/quote-reviews") @RequiredArgsConstructor
public class QuoteReviewController {
 private final QuoteService service;
 @GetMapping public PageResponse<QuoteSummaryResponse> findMyQuoteReviews(Principal actor,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
        @io.swagger.v3.oas.annotations.Parameter(hidden=true) @RequestParam Map<String,String> parameters) {
  if(!Set.of("page","size").containsAll(parameters.keySet())) throw new IllegalArgumentException("待審列表只接受 page 與 size，不可指定他人的 reviewerId。");
  return service.findMyQuoteReviews(actor.getName(), page, size);
 }
 @GetMapping("/{quoteId}") public QuoteResponse findMyQuoteReview(Principal actor, @PathVariable String quoteId) {
  return service.findMyQuoteReview(actor.getName(), quoteId);
 }
}
