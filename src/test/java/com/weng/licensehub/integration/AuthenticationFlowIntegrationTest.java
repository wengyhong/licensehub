package com.weng.licensehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthenticationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsInAndUsesSessionForProtectedRequest()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "alice@example.com",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isCreated());

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) csrfResult
                .getRequest()
                .getSession(false);

        CsrfToken csrfToken = (CsrfToken) csrfResult
                .getRequest()
                .getAttribute(CsrfToken.class.getName());

        assertThat(session).isNotNull();
        assertThat(csrfToken).isNotNull();

        String anonymousSessionId = session.getId();
        String anonymousCsrfToken = csrfToken.getToken();

        mockMvc.perform(post("/api/auth/login")
                .session(session)
                .header(
                        csrfToken.getHeaderName(),
                        anonymousCsrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "ALICE@example.com",
                          "password": "correct-horse-battery-staple"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("alice@example.com"));

        mockMvc.perform(get("/api/auth/me")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("alice@example.com"));
        assertThat(session.getId())
                .isNotEqualTo(anonymousSessionId);

        mockMvc.perform(get("/api/products")
                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                .session(session)
                .header(
                        csrfToken.getHeaderName(),
                        anonymousCsrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Rejected Product",
                          "description": "Uses the old CSRF token"
                        }
                        """))
                .andExpect(status().isForbidden());

        MvcResult refreshedCsrfResult = mockMvc.perform(get("/api/auth/csrf")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        CsrfToken authenticatedCsrfToken = (CsrfToken) refreshedCsrfResult
                .getRequest()
                .getAttribute(CsrfToken.class.getName());

        assertThat(authenticatedCsrfToken).isNotNull();

        String authenticatedCsrfTokenValue = authenticatedCsrfToken.getToken();
        mockMvc.perform(post("/api/products")
                .session(session)
                .header(
                        authenticatedCsrfToken.getHeaderName(),
                        authenticatedCsrfTokenValue)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Authenticated Product",
                          "description": "Uses the refreshed CSRF token"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products")
                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                .session(session)
                .header(
                        authenticatedCsrfToken.getHeaderName(),
                        authenticatedCsrfTokenValue))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}