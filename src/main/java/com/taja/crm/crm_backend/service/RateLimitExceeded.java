package com.taja.crm.crm_backend.service;

public class RateLimitExceeded extends RuntimeException {
    public static final String CODE = "RATE_LIMIT_EXCEEDED";
    public static final String DETAIL = "操作過於頻繁，請稍後再試。";
    private final long retryAfterSeconds;
    public RateLimitExceeded(long retryAfterSeconds) {
        super(DETAIL);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
