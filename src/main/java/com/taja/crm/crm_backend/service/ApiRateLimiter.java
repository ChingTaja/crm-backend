package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.config.RateLimitProperties;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiRateLimiter {
    public enum Scope { LOGIN_IP, LOGIN_ACCOUNT, FORGOT_IP, FORGOT_EMAIL, IMPORT_USER, GENERAL_IP, GENERAL_USER, AUTH_IP }
    private record Key(Scope scope, String digest) {}
    private static final class Bucket {
        double tokens;
        long updated;
        final long period;
        Bucket(double tokens, long now, long period) { this.tokens=tokens;this.updated=now;this.period=period; }
    }
    private final RateLimitProperties properties;
    private final LongSupplier ticker;
    private final Map<Key, Bucket> buckets = new HashMap<>();
    private long lastSweep;
    @Autowired public ApiRateLimiter(RateLimitProperties properties) { this(properties, System::nanoTime); }
    ApiRateLimiter(RateLimitProperties properties, LongSupplier ticker) { this.properties=properties;this.ticker=ticker;this.lastSweep=ticker.getAsLong(); }

    public synchronized void acquire(Scope scope, String subject) {
        if (!properties.isEnabled()) return;
        long now = ticker.getAsLong();
        if (now-lastSweep >= 60_000_000_000L || (buckets.size() >= properties.getMaxKeys() && now-lastSweep >= 1_000_000_000L)) {
            buckets.values().removeIf(b -> now-b.updated >= b.period);
            lastSweep = now;
        }
        var limit = limit(scope);
        long period = limit.getPeriod().toNanos();
        Key key = new Key(scope, hash(subject));
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            // Never evict active identities: doing so would reset another caller's quota.
            if (buckets.size() >= properties.getMaxKeys()) throw new RateLimitExceeded(1);
            bucket = new Bucket(limit.getCapacity(), now, period);
            buckets.put(key, bucket);
        }
        long elapsed = Math.max(0, now-bucket.updated);
        bucket.tokens = Math.min(limit.getCapacity(), bucket.tokens + elapsed * ((double) limit.getCapacity()/period));
        bucket.updated = now;
        if (bucket.tokens < 1) {
            long wait = (long) Math.ceil((1-bucket.tokens)*period/limit.getCapacity()/1_000_000_000d);
            throw new RateLimitExceeded(wait);
        }
        bucket.tokens -= 1;
    }
    private RateLimitProperties.Limit limit(Scope scope) {
        return switch(scope) {
            case LOGIN_IP -> properties.getLoginIp();
            case LOGIN_ACCOUNT -> properties.getLoginAccount();
            case FORGOT_IP -> properties.getForgotIp();
            case FORGOT_EMAIL -> properties.getForgotEmail();
            case IMPORT_USER -> properties.getImportUser();
            case GENERAL_IP -> properties.getGeneralIp();
            case GENERAL_USER -> properties.getGeneralUser();
            case AUTH_IP -> properties.getAuthIp();
        };
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
