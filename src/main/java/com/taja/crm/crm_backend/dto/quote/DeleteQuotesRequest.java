package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
import java.util.List;
public record DeleteQuotesRequest(@NotEmpty @Size(max = 100) List<@NotBlank String> ids) {}
