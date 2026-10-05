package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.quote.*;
import com.taja.crm.crm_backend.service.QuoteException;
import com.taja.crm.crm_backend.service.QuoteService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {
    private final QuoteService service;
    @GetMapping
    public PageResponse<QuoteSummaryResponse> findAllQuotes(Principal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.findAllQuotes(actor.getName(), Pagination.of(page, size));
    }
    @GetMapping("/{id}/reviewer-options")
    public java.util.List<ReviewerOption> reviewerOptions(Principal actor, @PathVariable String id,
            @RequestParam(required=false) String keyword) {
        return service.reviewerOptions(actor.getName(), id, keyword);
    }
    @GetMapping("/{id}")
    public QuoteResponse findByIdQuote(Principal actor, @PathVariable String id) { return service.findByIdQuote(actor.getName(), id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse createQuotes(Principal actor, @Valid @RequestBody CreateQuoteRequest request) { return service.createQuotes(actor.getName(), request); }
    @PutMapping("/{id}/versions/{versionId}")
    public QuoteResponse updateQuotes(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody UpdateQuoteRequest request) {
        return service.updateQuotes(actor.getName(), id, versionId, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuotes(Principal actor, @PathVariable String id) { service.deleteQuotes(actor.getName(), id); }
    @PostMapping("/{id}/versions/{versionId}/new-version")
    public QuoteResponse newVersion(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody QuoteActionRequest request) {
        return service.newVersion(actor.getName(), id, versionId, request);
    }
    @PostMapping("/{id}/versions/{versionId}/request-approval")
    public QuoteResponse requestApproval(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody RequestQuoteApprovalRequest request) {
        return service.requestApproval(actor.getName(), id, versionId, request);
    }
    @PostMapping("/{id}/versions/{versionId}/review")
    public QuoteResponse review(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody ReviewQuoteRequest request) {
        return service.review(actor.getName(), id, versionId, request);
    }
    @PostMapping("/{id}/versions/{versionId}/send")
    public QuoteResponse send(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody QuoteActionRequest request) {
        return service.send(actor.getName(), id, versionId, request);
    }
    @PostMapping("/{id}/versions/{versionId}/decision")
    public QuoteResponse decision(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody DecideQuoteRequest request) {
        return service.decision(actor.getName(), id, versionId, request);
    }
    @PostMapping("/{id}/versions/{versionId}/convert-to-order")
    public ConvertQuoteToOrderResponse convertToOrder(Principal actor, @PathVariable String id, @PathVariable String versionId, @Valid @RequestBody QuoteActionRequest request) {
        return service.convertToOrder(actor.getName(), id, versionId, request);
    }
    @ExceptionHandler(QuoteException.class)
    public ProblemDetail quoteError(QuoteException exception) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        result.setProperty("code", exception.getCode()); return result;
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ProblemDetail validationError(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException invalid
                && invalid.getBindingResult().getFieldError("reviewerId") != null) {
            ProblemDetail result = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "請選擇審核人。");
            result.setProperty("code", "QUOTE_REVIEWER_REQUIRED"); return result;
        }
        String detail = exception instanceof IllegalArgumentException ? exception.getMessage() : "報價欄位格式不正確，請確認必填欄位、日期、金額與明細。";
        ProblemDetail result = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        result.setProperty("code", "QUOTE_VALIDATION_ERROR"); return result;
    }
}
