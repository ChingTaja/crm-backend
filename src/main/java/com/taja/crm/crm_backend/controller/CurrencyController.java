package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.currency.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.taja.crm.crm_backend.service.CurrencyService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/currencies") @RequiredArgsConstructor
public class CurrencyController {
    private final CurrencyService currencies;

    @GetMapping
    public List<CurrencyResponse> findAllCurrencies(Principal principal) {
        return currencies.findAll(principal.getName());
    }
    @GetMapping("/{code}")
    public CurrencyResponse findByCode(@PathVariable String code, Principal principal) {
        return currencies.findByCode(principal.getName(), code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CurrencyResponse create(@Valid @RequestBody CreateCurrencyRequest request, Principal principal) {
        return currencies.create(principal.getName(), request);
    }

    @PutMapping("/{code}")
    public CurrencyResponse update(@PathVariable String code, @Valid @RequestBody UpdateCurrencyRequest request, Principal principal) {
        return currencies.update(principal.getName(), code, request);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code, Principal principal) {
        currencies.delete(principal.getName(), code);
        return ResponseEntity.noContent().build();
    }
}

