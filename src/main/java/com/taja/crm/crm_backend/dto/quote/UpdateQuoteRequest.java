package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class UpdateQuoteRequest extends CreateQuoteRequest {
    @NotNull @Positive private Long expectedRevision;
}
