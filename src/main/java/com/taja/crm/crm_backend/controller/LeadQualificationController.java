package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.lead.QualifyLeadRequest;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.service.LeadQualificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadQualificationController {
    private final LeadQualificationService qualificationService;

    @PostMapping("/{id}/qualification")
    public Lead qualifyLead(@PathVariable String id, @Valid @RequestBody QualifyLeadRequest request) {
        return qualificationService.qualifyLead(id, request);
    }
}
