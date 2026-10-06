package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.service.PasswordResetService;
import com.taja.crm.crm_backend.service.UserAuthService;
import java.security.Principal;
import com.taja.crm.crm_backend.service.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthController {
    private final PasswordResetService passwordResetService;
    private final UserAuthService userAuthService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request, Principal principal) {
        return userAuthService.register(request, null);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return jwtService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(Principal principal) {
        return userAuthService.findCurrentUser(principal.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Principal principal) {
        jwtService.logout(principal.getName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateUser() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "帳號或 email 已被使用");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request.email());
        return Map.of("message", "若此 email 已註冊，將收到密碼重設信件");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return Map.of("message", "密碼已重設");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MailException.class)
    public ProblemDetail handleMailFailure(MailException exception) {
        String code;
        String detail;
        if (exception instanceof com.taja.crm.crm_backend.service.MailConfigurationException) {
            code = "MAIL_NOT_CONFIGURED";
            detail = "寄信服務尚未完成設定，請聯絡系統管理員。";
            log.error("MAIL_NOT_CONFIGURED: check spring.mail.host/username/password and app.mail.from; restart after configuring.");
        } else if (exception instanceof org.springframework.mail.MailAuthenticationException) {
            code = "MAIL_AUTHENTICATION_FAILED";
            detail = "寄信服務驗證失敗，請聯絡系統管理員。";
            log.error("MAIL_AUTHENTICATION_FAILED: check SMTP account credentials.");
        } else {
            code = "MAIL_SEND_FAILED";
            detail = "寄信服務暫時無法使用，請稍後再試。";
            // Exception messages/stack traces can contain addresses, credentials or message contents.
            log.error("MAIL_SEND_FAILED: exceptionType={}, causeType={}",
                    exception.getClass().getSimpleName(),
                    exception.getCause() == null ? "none" : exception.getCause().getClass().getSimpleName());
        }
        ProblemDetail result = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        result.setProperty("code", code);
        return result;
    }
}
