package com.taja.crm.crm_backend.dto.auth;

@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"accessToken","tokenType","expiresIn","user"})
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
