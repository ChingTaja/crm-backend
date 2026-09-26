package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.metadata.FieldMetadata;
import com.taja.crm.crm_backend.service.EntityMetadataService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class EntityMetadataController {
    private final EntityMetadataService entityMetadataService;

    @GetMapping("/{entityName}/fields")
    public List<FieldMetadata> findFieldsByEntityName(@PathVariable String entityName) {
        return entityMetadataService.findFieldsByEntityName(entityName);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
