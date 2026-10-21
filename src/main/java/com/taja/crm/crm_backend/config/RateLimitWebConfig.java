package com.taja.crm.crm_backend.config;

import com.taja.crm.crm_backend.service.ApiRateLimiter;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import static com.taja.crm.crm_backend.service.ApiRateLimiter.Scope.*;

@Configuration @EnableConfigurationProperties(RateLimitProperties.class) @RequiredArgsConstructor
public class RateLimitWebConfig implements WebMvcConfigurer {
    private final ApiRateLimiter limiter;
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!(handler instanceof HandlerMethod) || "OPTIONS".equals(request.getMethod())) return true;
                String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                if (pattern == null || !pattern.startsWith("/api/") || request.getUserPrincipal() == null) return true;
                String actor = request.getUserPrincipal().getName();
                limiter.acquire(GENERAL_USER, actor);
                if ("POST".equals(request.getMethod()) && "/api/customers/import".equals(pattern)) limiter.acquire(IMPORT_USER, actor);
                return true;
            }
        }).addPathPatterns("/api/**").order(-10);
    }
}
