package com.taja.crm.crm_backend.dto.order;
import com.taja.crm.crm_backend.model.OrderStatus;
import jakarta.validation.constraints.*;
public record UpdateOrderStatusRequest(@NotNull OrderStatus status, @NotNull @Positive Long expectedRevision,
 @Size(max=10000) String reason) {}
