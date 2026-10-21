package com.taja.crm.crm_backend.dto.customer;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"idempotencyKey", "total", "successCount", "skippedCount", "failedCount", "results"})
public record CustomerImportResponse(String idempotencyKey, int total, long successCount,
        long skippedCount, long failedCount, List<CustomerImportResult> results) {
    public static CustomerImportResponse of(String key, List<CustomerImportResult> results) {
        return new CustomerImportResponse(key, results.size(), count(results, "success"),
                count(results, "skipped"), count(results, "failed"), List.copyOf(results));
    }
    private static long count(List<CustomerImportResult> results, String status) {
        return results.stream().filter(r -> status.equals(r.status())).count();
    }
}
