package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.search.EntitySearchRequest;
import com.taja.crm.crm_backend.service.EntitySearchService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EntitySearchController {
    private final EntitySearchService searchService;

    @PostMapping("/{entity}/search")
    public PageResponse<?> searchEntities(@PathVariable String entity, @RequestBody EntitySearchRequest request, Principal principal) {
        return searchService.search(entity, principal == null ? null : principal.getName(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
