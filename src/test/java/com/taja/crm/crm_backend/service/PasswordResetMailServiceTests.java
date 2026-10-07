package com.taja.crm.crm_backend.service;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PasswordResetMailServiceTests {
    @Test void missingCredentialsFailBeforeConnectingToSmtp() {
        JavaMailSenderImpl smtp=spy(new JavaMailSenderImpl());
        smtp.setHost("smtp.gmail.com");smtp.setUsername("");smtp.setPassword("");
        PasswordResetMailService service=new PasswordResetMailService(smtp,"https://example.com/reset","sender@example.com");
        assertThrows(MailConfigurationException.class,()->service.send("recipient@example.com","private-token"));
        verify(smtp,never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
    @Test void missingFromAddressFailsBeforeSending() {
        var smtp=mock(org.springframework.mail.javamail.JavaMailSender.class);
        var service=new PasswordResetMailService(smtp,"https://example.com/reset","");
        assertThrows(MailConfigurationException.class,()->service.send("recipient@example.com","private-token"));
        verifyNoInteractions(smtp);
    }
}
