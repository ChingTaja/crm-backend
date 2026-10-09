package com.taja.crm.crm_backend.dto.auth;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"accessToken","tokenType","expiresIn"})
public record RefreshResponse(String accessToken, String tokenType, long expiresIn) {}
