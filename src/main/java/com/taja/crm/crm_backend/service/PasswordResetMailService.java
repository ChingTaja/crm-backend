package com.taja.crm.crm_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetMailService {
    private final JavaMailSender sender;
    private final String resetUrl;
    private final String from;

    public PasswordResetMailService(JavaMailSender sender,
            @Value("${app.auth.reset-password-url}") String resetUrl,
            @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.resetUrl = resetUrl;
        this.from = from;
    }

    public void send(String email, String token) {
        String link = UriComponentsBuilder.fromUriString(resetUrl)
                .replaceQueryParam("token", token).build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("CRM 密碼重設");
        message.setText("請開啟以下連結重設密碼，連結將於 15 分鐘後失效，且僅能使用一次。\n\n"
                + link + "\n\n若您未申請重設密碼，請忽略此信。");
        sender.send(message);
    }
}
