package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.lead.CreateLeadRequest;
import com.taja.crm.crm_backend.dto.lead.CreateLeadResponse;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.service.LeadService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
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
    public List<Lead> findAll() {
        return leadService.findAll();
    }

    @GetMapping("/{id}")
    public Lead findById(@PathVariable String id) {
        return leadService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CreateLeadResponse> create(@Valid @RequestBody CreateLeadRequest request) {
        Lead created = leadService.create(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateLeadResponse.fromEntity(created));
    }

    /** 完整更新；省略 qualification 時會清除原本的審核資料。 */
    @PutMapping("/{id}")
    public Lead update(@PathVariable String id, @Valid @RequestBody Lead lead) {
        return leadService.update(id, lead);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        leadService.delete(id);
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
