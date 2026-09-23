package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.model.LeadQualification;
import com.taja.crm.crm_backend.repo.LeadRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTests {

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private LeadService leadService;

    @Test
    void createRejectsExistingIdToPreventOverwriting() {
        Lead lead = new Lead();
        lead.setId("existing-id");

        assertThrows(IllegalArgumentException.class, () -> leadService.create(lead));
        verifyNoInteractions(leadRepository);
    }

    @Test
    void updatePreservesTargetIdAndClearsOmittedQualification() {
        Lead existing = new Lead();
        existing.setId("target-id");
        existing.setQualification(new LeadQualification());
        Lead replacement = new Lead();
        replacement.setId("different-id");
        replacement.setName("新名稱");
        when(leadRepository.findById("target-id")).thenReturn(Optional.of(existing));
        when(leadRepository.save(existing)).thenReturn(existing);

        Lead result = leadService.update("target-id", replacement);

        assertEquals("target-id", result.getId());
        assertEquals("新名稱", result.getName());
        assertNull(result.getQualification());
    }

    @Test
    void missingLeadCannotBeUpdatedOrDeleted() {
        when(leadRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> leadService.update("missing", new Lead()));
        assertThrows(EntityNotFoundException.class, () -> leadService.delete("missing"));
        verify(leadRepository, never()).save(any(Lead.class));
        verify(leadRepository, never()).delete(any(Lead.class));
    }
}
