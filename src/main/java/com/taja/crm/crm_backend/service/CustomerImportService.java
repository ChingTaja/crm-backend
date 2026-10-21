package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.customer.*;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Savepoint;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service @RequiredArgsConstructor
public class CustomerImportService {
    private final PermissionService access;
    private final JdbcTemplate jdbc;
    private final Validator validator;
    private final ObjectMapper json;

    @Transactional
    public CustomerImportResponse importCustomers(String actorId, String suppliedKey, CustomerImportRequest request) {
        access.require(actorId, "customers.create");
        validateRequest(request);
        String key = suppliedKey == null ? UUID.randomUUID().toString() : suppliedKey;
        if (!key.matches("[A-Za-z0-9._:-]{1,128}"))
            throw error(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key 須為 1 至 128 碼英數字或 . _ : -。");
        String hash = hash(json.writeValueAsString(request));
        // Transaction-scoped lock serializes identical keys across instances. Crashes release the lock.
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", "customer-import:" + actorId + ":" + key);
        var stored = jdbc.queryForList("select request_hash,response_json from customer_import_requests where actor_id=? and idempotency_key=?", actorId, key);
        if (!stored.isEmpty()) {
            if (!hash.equals(stored.getFirst().get("request_hash")))
                throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "此 Idempotency-Key 已用於不同匯入內容，請使用新的 Key。");
            return json.readValue((String) stored.getFirst().get("response_json"), CustomerImportResponse.class);
        }
        // Serialize all customer writes, including ordinary CRUD and Lead conversion, while checking duplicates.
        // SELECT reads remain available. No global uniqueness constraint is imposed on historical customer data.
        jdbc.execute("lock table customers in share row exclusive mode");
        List<CustomerImportResult> results = new ArrayList<>();
        for (CustomerImportRow raw : request.rows()) results.add(importRow(normalize(raw)));
        CustomerImportResponse response = CustomerImportResponse.of(key, results);
        // Customer inserts and this replay result commit atomically. There is no committed 'in progress' record.
        jdbc.update("insert into customer_import_requests(actor_id,idempotency_key,request_hash,response_json) values (?,?,?,?)",
                actorId, key, hash, json.writeValueAsString(response));
        return response;
    }

    private CustomerImportResult importRow(CustomerImportRow row) {
        var violations = validator.validate(row);
        if (!violations.isEmpty()) {
            String reason = violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted()
                    .collect(java.util.stream.Collectors.joining("；"));
            return result(row, "failed", "VALIDATION_ERROR", reason, null);
        }
        return jdbc.execute((ConnectionCallback<CustomerImportResult>) connection -> {
            Savepoint point = connection.setSavepoint();
            try {
                boolean sameName = Boolean.TRUE.equals(jdbc.queryForObject(
                        "select exists(select 1 from customers where btrim(name)=? and coalesce(btrim(company),'')=?)",
                        Boolean.class, row.name(), row.company() == null ? "" : row.company()));
                if (sameName) return result(row, "skipped", "DUPLICATE_NAME_COMPANY", "名稱與公司已存在，略過且不覆寫。", null);
                if (row.email() != null && Boolean.TRUE.equals(jdbc.queryForObject(
                        "select exists(select 1 from customers where lower(btrim(email))=lower(?))", Boolean.class, row.email())))
                    return result(row, "skipped", "DUPLICATE_EMAIL", "Email 已存在，略過且不覆寫。", null);
                String id = UUID.randomUUID().toString();
                jdbc.update("insert into customers(id,name,company,email,phone,owner) values (?,?,?,?,?,?)",
                        id, row.name(), row.company(), row.email(), row.phone(), row.owner());
                return result(row, "success", "CUSTOMER_IMPORTED", "匯入成功。", id);
            } catch (DataAccessException failure) {
                // Recover PostgreSQL's aborted statement state without losing successful rows.
                connection.rollback(point);
                return result(row, "failed", "ROW_PERSISTENCE_FAILED", "此列無法儲存，請確認資料後以新的 Key 重試。", null);
            } finally {
                connection.releaseSavepoint(point);
            }
        });
    }

    private void validateRequest(CustomerImportRequest request) {
        if (request == null || !validator.validate(request).isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "CUSTOMER_IMPORT_INVALID_ROWS", "rows 須包含 1 至 500 筆資料。");
        Set<Integer> numbers = new HashSet<>();
        for (CustomerImportRow row : request.rows()) {
            if (row == null || row.rowNumber() == null || row.rowNumber() <= 0 || !numbers.add(row.rowNumber()))
                throw error(HttpStatus.BAD_REQUEST, "CUSTOMER_IMPORT_INVALID_ROWS", "每列須有不重複的正整數 rowNumber。");
        }
    }
    private CustomerImportRow normalize(CustomerImportRow row) {
        return new CustomerImportRow(row.rowNumber(), trim(row.name()), trim(row.company()),
                trim(row.email()), trim(row.phone()), trim(row.owner()));
    }
    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
    private CustomerImportResult result(CustomerImportRow row, String status, String code, String reason, String id) {
        return new CustomerImportResult(row.rowNumber(), status, code, reason, id);
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private QuoteException error(HttpStatus status, String code, String detail) { return new QuoteException(status, code, detail); }
}
