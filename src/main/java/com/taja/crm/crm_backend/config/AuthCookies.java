package com.taja.crm.crm_backend.config;
import com.taja.crm.crm_backend.service.RefreshTokenService.Grant;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
@Component
public class AuthCookies {
    public static final String REFRESH="CRM-REFRESH", CSRF="CRM-CSRF", PATH="/api/auth";
    private final boolean secure;
    private final String sameSite;
    private final Clock clock;
    public AuthCookies(@Value("${app.auth.cookie.secure:true}") boolean secure,
            @Value("${app.auth.cookie.same-site:Lax}") String sameSite, Clock clock) {
        if(!Set.of("Strict","Lax","None").contains(sameSite) || (sameSite.equals("None")&&!secure))
            throw new IllegalArgumentException("Invalid auth cookie SameSite/Secure settings");
        this.secure=secure;this.sameSite=sameSite;this.clock=clock;
    }
    public boolean secure(){return secure;}
    public String sameSite(){return sameSite;}
    public void set(HttpServletResponse response,Grant grant){write(response,grant.refreshToken(),Duration.between(clock.instant(),grant.expiresAt()));}
    public void clear(HttpServletResponse response){write(response,"",Duration.ZERO);}
    private void write(HttpServletResponse response,String value,Duration maxAge){
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(REFRESH,value).httpOnly(true).secure(secure)
                .sameSite(sameSite).path(PATH).maxAge(maxAge).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
    }
}
