package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record QuoteLineRequest(String id, @NotBlank String productId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal discountPercent,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal taxPercent) {}
