package com.taja.crm.crm_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class RefreshSecurityConfig {
    private final List<String> origins;
    public RefreshSecurityConfig(@Value("${app.auth.allowed-origins:http://localhost:5173,http://localhost:8080}") String configured) {
        origins=Arrays.stream(configured.split(",")).map(String::strip).filter(s->!s.isEmpty()).toList();
        if(origins.isEmpty() || origins.stream().anyMatch(s->s.contains("*")||s.equals("null")))
            throw new IllegalArgumentException("Explicit auth allowed origins are required");
    }
    @Bean public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors=new CorsConfiguration();cors.setAllowedOrigins(origins);cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization","Content-Type","X-CSRF-TOKEN"));
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);return source;
    }
    @Bean @Order(1)
    public SecurityFilterChain cookieAuthChain(HttpSecurity http,AuthCookies cookies) throws Exception {
        CookieCsrfTokenRepository repository=new CookieCsrfTokenRepository();
        repository.setCookieName(AuthCookies.CSRF);repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookieCustomizer(builder->builder.httpOnly(true).secure(cookies.secure()).sameSite(cookies.sameSite()).path(AuthCookies.PATH));
        CsrfTokenRequestAttributeHandler handler=new CsrfTokenRequestAttributeHandler() {
            @Override public String resolveCsrfTokenValue(HttpServletRequest request,CsrfToken token) {
                return request.getHeader(token.getHeaderName()); // Never accept tokens in URLs or form fields.
            }
        };
        return http.securityMatcher("/api/auth/login","/api/auth/csrf","/api/auth/refresh","/api/auth/logout")
            .cors(cors->{})
            .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache->cache.disable()).logout(logout->logout.disable())
            .csrf(csrf->csrf.csrfTokenRepository(repository).csrfTokenRequestHandler(handler))
            .exceptionHandling(errors->errors.accessDeniedHandler((request,response,error)->
                    SecurityProblem.write(response,403,"CSRF_INVALID","CSRF 驗證失敗，請重新取得驗證碼。")))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                    String origin=request.getHeader("Origin");
                    if("POST".equals(request.getMethod()) && origin!=null && !origins.contains(origin)) {
                        SecurityProblem.write(response,403,"CSRF_INVALID","不允許此來源的請求。");return;
                    }
                    chain.doFilter(request,response);
                }
            },CsrfFilter.class)
            .authorizeHttpRequests(auth->auth.anyRequest().permitAll())
            // Intentionally no bearer filter: expired/invalid Authorization cannot block refresh or logout.
            .build();
    }
}
