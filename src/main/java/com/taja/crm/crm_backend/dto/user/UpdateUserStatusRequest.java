package com.taja.crm.crm_backend.dto.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Boolean enabled) {}
