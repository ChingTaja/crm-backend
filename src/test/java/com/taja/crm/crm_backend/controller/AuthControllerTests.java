package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.service.PasswordResetService;
import com.taja.crm.crm_backend.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean PasswordResetService service;
    @MockitoBean UserAuthService userAuthService;
    @MockitoBean com.taja.crm.crm_backend.service.JwtService jwtService;

    @Test
    void forgotPasswordAcceptsEmailAndReturnsGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@gmail.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").isNotEmpty());
        verify(service).forgotPassword("user@gmail.com");
    }

    @Test
    void invalidEmailAndShortPasswordAreRejected() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "a".repeat(43) + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void resetsPassword() throws Exception {
        String token = "a".repeat(43);
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isOk());
        verify(service).resetPassword(token, "new-password");
    }

    @Test
    void usedTokenReturnsBadRequest() throws Exception {
        String token = "a".repeat(43);
        doThrow(new IllegalArgumentException("重設連結無效、已過期或已使用"))
                .when(service).resetPassword(token, "new-password");
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isBadRequest());
    }
}
