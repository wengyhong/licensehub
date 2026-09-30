package com.weng.licensehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserRegistrationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersUserAndStoresPasswordHash()
            throws Exception {

        String rawPassword =
                "correct-horse-battery-staple";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "Alice@example.com",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.email")
                        .value("Alice@example.com"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.password")
                        .doesNotExist())
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        UserAccount saved = userAccountRepository
                .findByEmailIgnoreCase("alice@example.com")
                .orElseThrow();

        assertThat(saved.getPasswordHash())
                .isNotEqualTo(rawPassword);

        assertThat(saved.getPasswordHash())
                .startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(
                rawPassword,
                saved.getPasswordHash()))
                .isTrue();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "ALICE@example.com",
                          "password": "another-valid-password"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Email already registered"));
    }
}