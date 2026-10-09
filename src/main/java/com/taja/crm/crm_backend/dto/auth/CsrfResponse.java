package com.taja.crm.crm_backend.dto.auth;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"token","headerName"})
public record CsrfResponse(String token, String headerName) {}
