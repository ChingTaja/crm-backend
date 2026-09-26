package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.service.PasswordResetService;
import com.taja.crm.crm_backend.service.UserAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final PasswordResetService passwordResetService;
    private final UserAuthService userAuthService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userAuthService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserResponse user = userAuthService.login(request);
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        httpRequest.getSession(true).setAttribute("userId", user.id());
        return user;
    }

    @GetMapping("/me")
    public UserResponse currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof String userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "尚未登入");
        }
        return userAuthService.findCurrentUser(userId);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
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
    public ProblemDetail handleMailFailure() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "寄信服務暫時無法使用，請稍後再試");
    }
}
