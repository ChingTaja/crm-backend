package com.taja.crm.crm_backend.config;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
public final class SecurityProblem {
    private SecurityProblem() {}
    public static void unauthorized(HttpServletResponse response, AuthenticationException exception) throws IOException {
        String code="ACCESS_TOKEN_INVALID";
        for(Throwable cause=exception;cause!=null;cause=cause.getCause()) {
            if(cause instanceof JwtValidationException invalid) {
                code=invalid.getErrors().stream().map(e->e.getErrorCode())
                    .filter(Set.of("ACCESS_TOKEN_EXPIRED","ACCOUNT_DISABLED","ACCESS_TOKEN_REVOKED")::contains)
                    .findFirst().orElse(code);
            }
        }
        String detail=switch(code){
            case "ACCESS_TOKEN_EXPIRED" -> "登入憑證已過期，請嘗試續期。";
            case "ACCOUNT_DISABLED" -> "帳號已停用，請聯絡管理員。";
            default -> "登入憑證無效，請重新登入。";
        };
        response.setHeader("WWW-Authenticate","Bearer error=\"invalid_token\"");
        write(response,401,code,detail);
    }
    public static void write(HttpServletResponse response,int status,String code,String detail) throws IOException {
        response.setStatus(status);response.setContentType("application/problem+json");response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store");
        // All supplied codes/details are fixed server strings, never exception text or user input.
        response.getWriter().write("{\"status\":"+status+",\"title\":\""+(status==401?"Unauthorized":"Forbidden")+"\",\"code\":\""+code+"\",\"detail\":\""+detail+"\"}");
    }
}
