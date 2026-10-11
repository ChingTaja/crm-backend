package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CreateQuoteRequest {
    @NotBlank @Size(max = 255) private String name;
    @NotBlank private String customerId;
    @NotBlank private String opportunityId;
    @NotEmpty @Size(max = 200) private List<@NotNull @Valid QuoteLineRequest> lines;
    @Size(max = 10000) private String paymentTerms;
    @Size(max = 10000) private String deliveryTerms;
    @Size(max = 10000) private String warranty;
    @Size(max = 10000) private String notes;
}
