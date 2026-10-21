package com.taja.crm.crm_backend.config;

import com.taja.crm.crm_backend.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import static com.taja.crm.crm_backend.service.ApiRateLimiter.Scope.*;

// Created only within the security chains, so it is not auto-registered as a second servlet filter.
public class RateLimitIpFilter extends OncePerRequestFilter {
    private final ApiRateLimiter limiter;
    public RateLimitIpFilter(ApiRateLimiter limiter) { this.limiter=limiter; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (!path.startsWith("/api/") || "OPTIONS".equals(request.getMethod())) { chain.doFilter(request,response);return; }
        try {
            // Intentionally ignore client-supplied X-Forwarded-For / Forwarded headers.
            // A trusted deployment proxy must provide the correct remote address at the server boundary.
            String ip = request.getRemoteAddr();
            if ("POST".equals(request.getMethod()) && "/api/auth/login".equals(path)) limiter.acquire(LOGIN_IP, ip);
            else if ("POST".equals(request.getMethod()) && "/api/auth/forgot-password".equals(path)) limiter.acquire(FORGOT_IP, ip);
            else if (path.equals("/api/auth/csrf") || path.equals("/api/auth/refresh") || path.equals("/api/auth/logout")) limiter.acquire(AUTH_IP, ip);
            else limiter.acquire(GENERAL_IP, ip);
        } catch (RateLimitExceeded limited) {
            response.setHeader("Retry-After", Long.toString(limited.getRetryAfterSeconds()));
            SecurityProblem.write(response,429,RateLimitExceeded.CODE,RateLimitExceeded.DETAIL);
            return;
        }
        chain.doFilter(request,response);
    }
}
