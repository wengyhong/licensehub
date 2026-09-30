package com.weng.licensehub.user.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.user.application.EmailAlreadyRegisteredException;
import com.weng.licensehub.user.application.UserRegistrationService;
import com.weng.licensehub.user.domain.UserAccount;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService registrationService;

    @Test
    void registerReturns201AndSafeUserResponse()
            throws Exception {

        UUID id = UUID.fromString(
                "7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

        Instant createdAt =
                Instant.parse("2026-09-30T10:00:00Z");

        UserAccount account = mock(UserAccount.class);

        when(account.getId()).thenReturn(id);
        when(account.getEmail())
                .thenReturn("alice@example.com");
        when(account.getCreatedAt())
                .thenReturn(createdAt);

        when(registrationService.register(
                "alice@example.com",
                "correct-horse-battery-staple"))
                .thenReturn(account);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "alice@example.com",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(id.toString()))
                .andExpect(jsonPath("$.email")
                        .value("alice@example.com"))
                .andExpect(jsonPath("$.createdAt")
                        .value(createdAt.toString()))
                .andExpect(jsonPath("$.password")
                        .doesNotExist())
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        verify(registrationService).register(
                "alice@example.com",
                "correct-horse-battery-staple");
    }

    @Test
    void registerReturns400ForInvalidEmail()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "not-an-email",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationService);
    }

    @Test
    void registerReturns400ForShortPassword()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "alice@example.com",
                          "password": "short"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationService);
    }

    @Test
    void registerReturns409ForDuplicateEmail()
            throws Exception {

        when(registrationService.register(
                "alice@example.com",
                "correct-horse-battery-staple"))
                .thenThrow(
                        new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "alice@example.com",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title")
                        .value("Email already registered"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "An account with this email already exists"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/auth/register"));
    }
}