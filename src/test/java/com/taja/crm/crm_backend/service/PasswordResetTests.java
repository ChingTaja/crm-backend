package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"app.mail.from=crm@example.com", "app.auth.reset-password-url=https://frontend.example/reset-password"})
@Transactional
class PasswordResetTests {
    @Autowired PasswordResetService service;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @MockitoBean JavaMailSender sender;
    User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUsername("account-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPasswordHash(encoder.encode("initial-password"));
        users.saveAndFlush(user);
    }

    String issue() {
        service.forgotPassword(user.getEmail());
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, atLeastOnce()).send(message.capture());
        String body = message.getValue().getText();
        assertTrue(body.contains("https://frontend.example/reset-password?token="));
        assertEquals(user.getEmail(), message.getValue().getTo()[0]);
        return body.split("token=")[1].split("\\s")[0];
    }

    @Test
    void storesOnlyHashAndResetsOnce() {
        String raw = issue();
        var token = tokens.findByTokenHash(PasswordResetService.hash(raw)).orElseThrow();
        assertNotEquals(raw, token.getTokenHash());
        assertTrue(token.getExpiresAt().isAfter(Instant.now().plusSeconds(890)));
        service.resetPassword(raw, "new-password-1");
        em.flush(); em.clear();
        assertTrue(encoder.matches("new-password-1", users.findById(user.getId()).orElseThrow().getPasswordHash()));
        assertTrue(tokens.findByTokenHash(PasswordResetService.hash(raw)).orElseThrow().isUsed());
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(raw, "new-password-2"));
    }

    @Test
    void expiredUnknownAndSupersededTokensAreRejected() {
        String first = issue();
        String second = issue();
        em.flush(); em.clear();
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(first, "new-password-1"));
        var token = tokens.findByTokenHash(PasswordResetService.hash(second)).orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        em.flush();
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(second, "new-password-1"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("unknown", "new-password-1"));
    }

    @Test
    void rejectsCurrentAndThreePreviousPasswords() {
        user.getPasswordHistory().add(encoder.encode("previous-one"));
        user.getPasswordHistory().add(encoder.encode("previous-two"));
        user.getPasswordHistory().add(encoder.encode("previous-three"));
        String raw = issue();
        for (String password : new String[]{"initial-password", "previous-one", "previous-two", "previous-three"}) {
            assertThrows(IllegalArgumentException.class, () -> service.resetPassword(raw, password));
        }
        assertFalse(tokens.findByTokenHash(PasswordResetService.hash(raw)).orElseThrow().isUsed());
        service.resetPassword(raw, "brand-new-password");
        assertEquals(3, user.getPasswordHistory().size());
        assertTrue(encoder.matches("initial-password", user.getPasswordHistory().getLast()));
    }

    @Test
    void unknownEmailDoesNotSendMail() {
        service.forgotPassword("unknown@example.com");
        verifyNoInteractions(sender);
    }

    @Test
    void rejectsPasswordsExceedingBcryptByteLimit() {
        String raw = issue();
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(raw, "密".repeat(25)));
    }
}
