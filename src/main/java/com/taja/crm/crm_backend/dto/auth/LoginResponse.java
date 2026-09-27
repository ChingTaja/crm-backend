package com.taja.crm.crm_backend.dto.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
