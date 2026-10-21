package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.customer.*;
import com.taja.crm.crm_backend.service.CustomerImportService;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/customers") @RequiredArgsConstructor
public class CustomerImportController {
    private final CustomerImportService imports;

    @PostMapping("/import")
    @Operation(summary = "批次匯入客戶", description = "需要 customers.create。rows 為 1–500 筆且 rowNumber 為不重複正整數；欄位錯誤逐列回傳，部分成功回 200。Idempotency-Key 選填但建議於首次送出前產生並於重試時沿用；同一使用者同 Key 不同內容回 409。")
    public ResponseEntity<CustomerImportResponse> importCustomers(Principal actor,
            @io.swagger.v3.oas.annotations.Parameter(description = "建議首次送出前產生 UUID；重送沿用原 Key 及內容。作用範圍為登入者與此 API。",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(maxLength = 128, pattern = "[A-Za-z0-9._:-]{1,128}"))
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestBody CustomerImportRequest request) {
        var result = imports.importCustomers(actor.getName(), key, request);
        return ResponseEntity.ok().header("Idempotency-Key", result.idempotencyKey()).body(result);
    }
}
