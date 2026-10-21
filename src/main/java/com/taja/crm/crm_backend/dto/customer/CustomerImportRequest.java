package com.taja.crm.crm_backend.dto.customer;

import jakarta.validation.constraints.*;
import java.util.List;

public record CustomerImportRequest(
        @NotNull @Size(min = 1, max = 500) List<CustomerImportRow> rows) {}
