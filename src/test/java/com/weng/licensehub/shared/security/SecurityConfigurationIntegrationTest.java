package com.weng.licensehub.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
  @Autowired private UserAccountRepository userAccountRepository;

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mockMvc;

  @Test
  void anonymousUserCannotAccessProducts() throws Exception {

    mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanAccessProducts() throws Exception {

    mockMvc
        .perform(get("/api/products").with(user("manager@example.com").roles("USER")))
        .andExpect(status().isOk());
  }

  @Test
  void authenticatedPostWithoutCsrfIsRejected() throws Exception {

    mockMvc
        .perform(
            post("/api/products")
                .with(user("manager@example.com").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "name": "Security Test Product",
                                                  "description": "Missing CSRF token"
                                                }
                                                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void authenticatedPostWithCsrfIsAllowed() throws Exception {

    mockMvc
        .perform(
            post("/api/products")
                .with(user("manager@example.com").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "name": "Security Test Product",
                                                  "description": "Valid CSRF token"
                                                }
                                                """))
        .andExpect(status().isCreated());
  }

  @Test
  void registrationIsPublicAndDoesNotRequireCsrf() throws Exception {

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "email": "not-an-email",
                                                  "password": "short"
                                                }
                                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void healthEndpointIsPublic() throws Exception {

    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void csrfTokenEndpointIsPublic() throws Exception {

    mockMvc
        .perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.parameterName").value("_csrf"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void currentUserEndpointRequiresAuthentication() throws Exception {

    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @BeforeEach
  void ensureManagerExists() {

    if (userAccountRepository.findByEmailIgnoreCase("manager@example.com").isEmpty()) {

      userAccountRepository.save(new UserAccount("manager@example.com", "{noop}unused"));
    }
  }

  @Test
  void activationHistoryRequiresAuthentication() throws Exception {
    UUID licenseId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/licenses/{licenseId}/activations", licenseId))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanRequestActivationHistory() throws Exception {
    UUID licenseId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/licenses/{licenseId}/activations", licenseId)
                .with(user("manager@example.com").roles("USER")))
        .andExpect(status().isNotFound());
  }

  @Test
  void anonymousUserCannotDeactivateManagedActivation() throws Exception {

    UUID licenseId = UUID.randomUUID();
    UUID activationId = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/api/licenses/{licenseId}/activations/{activationId}/deactivate",
                    licenseId,
                    activationId)
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managementDeactivationRequiresCsrf() throws Exception {
    UUID licenseId = UUID.randomUUID();
    UUID activationId = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/api/licenses/{licenseId}/activations/{activationId}/deactivate",
                    licenseId,
                    activationId)
                .with(user("manager@example.com").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void authenticatedUserWithCsrfCanRequestManagementDeactivation() throws Exception {

    UUID licenseId = UUID.randomUUID();
    UUID activationId = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/api/licenses/{licenseId}/activations/{activationId}/deactivate",
                    licenseId,
                    activationId)
                .with(user("manager@example.com").roles("USER"))
                .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void validationEndpointIsPublicAndDoesNotRequireCsrf() throws Exception {

    mockMvc
        .perform(
            post("/api/activations/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "licenseKey": "not-a-valid-license-key",
                                                  "machineFingerprint": "test-machine"
                                                }
                                                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid license key"))
        .andExpect(jsonPath("$.detail").value("Invalid license key"));
  }

  @Test
  void openApiDocumentIsPublic() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").isNotEmpty())
        .andExpect(jsonPath("$.info.title").value("LicenseHub API"))
        .andExpect(jsonPath("$.info.version").value("v1"))
        .andExpect(jsonPath("$.paths").exists());
  }
}
