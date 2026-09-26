package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.lead.CreateLeadRequest;
import com.taja.crm.crm_backend.dto.lead.CreateLeadResponse;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.service.LeadService;
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
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public PageResponse<Lead> findAllLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(leadService.findAllLeads(Pagination.of(page, size)));
    }

    @GetMapping("/{id}")
    public Lead findByIdLead(@PathVariable String id) {
        return leadService.findByIdLead(id);
    }

    @PostMapping
    public ResponseEntity<CreateLeadResponse> createLeads(@Valid @RequestBody CreateLeadRequest request) {
        Lead created = leadService.createLeads(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateLeadResponse.fromEntity(created));
    }

    /** 完整更新；省略 qualification 時會清除原本的審核資料。 */
    @PutMapping("/{id}")
    public Lead updateLeads(@PathVariable String id, @Valid @RequestBody Lead lead) {
        return leadService.updateLeads(id, lead);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeads(@PathVariable String id) {
        leadService.deleteLeads(id);
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
