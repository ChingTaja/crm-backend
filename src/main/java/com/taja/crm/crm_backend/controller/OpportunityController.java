package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.opportunity.CreateOpportunityRequest;
import com.taja.crm.crm_backend.dto.opportunity.OpportunityResponse;
import com.taja.crm.crm_backend.dto.opportunity.UpdateOpportunityRequest;
import com.taja.crm.crm_backend.model.Opportunity;
import com.taja.crm.crm_backend.service.OpportunityService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.Pagination;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public PageResponse<OpportunityResponse> findAllOpportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(opportunityService.findAllOpportunities(Pagination.of(page, size)).map(OpportunityResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public OpportunityResponse findByIdOpportunity(@PathVariable String id) {
        return OpportunityResponse.fromEntity(opportunityService.findByIdOpportunity(id));
    }

    @PostMapping
    public ResponseEntity<OpportunityResponse> createOpportunities(@Valid @RequestBody CreateOpportunityRequest request) {
        Opportunity created = opportunityService.createOpportunities(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(OpportunityResponse.fromEntity(created));
    }

    /** 完整更新；未提供的欄位會清空。 */
    @PutMapping("/{id}")
    public OpportunityResponse updateOpportunities(@PathVariable String id, @Valid @RequestBody UpdateOpportunityRequest request) {
        return OpportunityResponse.fromEntity(opportunityService.updateOpportunities(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpportunities(@PathVariable String id) {
        opportunityService.deleteOpportunities(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
