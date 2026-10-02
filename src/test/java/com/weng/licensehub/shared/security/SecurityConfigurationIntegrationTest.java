package com.weng.licensehub.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityConfigurationIntegrationTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Test
        void anonymousUserCannotAccessProducts()
                        throws Exception {

                mockMvc.perform(get("/api/products"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void authenticatedUserCanAccessProducts()
                        throws Exception {

                mockMvc.perform(get("/api/products")
                                .with(user("manager@example.com")
                                                .roles("USER")))
                                .andExpect(status().isOk());
        }

        @Test
        void authenticatedPostWithoutCsrfIsRejected()
                        throws Exception {

                mockMvc.perform(post("/api/products")
                                .with(user("manager@example.com")
                                                .roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Security Test Product",
                                                  "description": "Missing CSRF token"
                                                }
                                                """))
                                .andExpect(status().isForbidden());
        }

        @Test
        void authenticatedPostWithCsrfIsAllowed()
                        throws Exception {

                mockMvc.perform(post("/api/products")
                                .with(user("manager@example.com")
                                                .roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Security Test Product",
                                                  "description": "Valid CSRF token"
                                                }
                                                """))
                                .andExpect(status().isCreated());
        }

        @Test
        void registrationIsPublicAndDoesNotRequireCsrf()
                        throws Exception {

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "email": "not-an-email",
                                                  "password": "short"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void healthEndpointIsPublic()
                        throws Exception {

                mockMvc.perform(get("/actuator/health"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status")
                                                .value("UP"));
        }

        @Test
        void csrfTokenEndpointIsPublic()
                        throws Exception {

                mockMvc.perform(get("/api/auth/csrf"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.headerName")
                                                .value("X-CSRF-TOKEN"))
                                .andExpect(jsonPath("$.parameterName")
                                                .value("_csrf"))
                                .andExpect(jsonPath("$.token")
                                                .isNotEmpty());
        }

        @Test
        void currentUserEndpointRequiresAuthentication()
                        throws Exception {

                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isUnauthorized());
        }
}