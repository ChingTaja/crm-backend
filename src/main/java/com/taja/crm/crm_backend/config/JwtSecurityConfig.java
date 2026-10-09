package com.taja.crm.crm_backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.taja.crm.crm_backend.repo.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class JwtSecurityConfig {
    @Bean
    public SecretKey jwtSigningKey(@Value("${app.jwt.secret:}") String configured) {
        byte[] bytes;
        if (configured.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(configured);
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET 必須為至少 32 bytes 的 Base64 金鑰");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, UserRepository users,
            @Value("${app.jwt.issuer}") String issuer, java.time.Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(jwt -> {
            Object version=jwt.getClaims().get("version");
            String code=null;
            var now=clock.instant();
            if(!issuer.equals(jwt.getClaims().get("iss")) || jwt.getSubject()==null
                    || jwt.getExpiresAt()==null || jwt.getIssuedAt()==null || !(version instanceof Number)
                    || jwt.getIssuedAt().isAfter(now.plusSeconds(60))
                    || (jwt.getNotBefore()!=null && jwt.getNotBefore().isAfter(now))) code="ACCESS_TOKEN_INVALID";
            else {
                var user=users.findById(jwt.getSubject()).orElse(null);
                if(user==null) code="ACCESS_TOKEN_INVALID";
                else if(!user.isEnabled()) code="ACCOUNT_DISABLED";
                else if(user.getTokenVersion()!=((Number)version).longValue()) code="ACCESS_TOKEN_REVOKED";
                else if(!jwt.getExpiresAt().isAfter(now)) code="ACCESS_TOKEN_EXPIRED";
            }
            return code==null?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(code,"登入憑證驗證失敗",null));
        });
        return decoder;
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> {})
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request,response,error) -> SecurityProblem.unauthorized(response,error)))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register",
                                "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(jwt -> {}).authenticationEntryPoint((request,response,error) -> SecurityProblem.unauthorized(response,error)))
                .build();
    }
}
