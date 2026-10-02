package com.weng.licensehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ActivationFlowIntegrationTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void completesActivationLifecycle() throws Exception {
                String productId = createProduct();

                String licenseKey = issueLicense(productId);

                String firstActivationId = activateMachine(
                                licenseKey,
                                "integration-machine-001",
                                "Integration laptop");

                String repeatedActivationId = activateMachine(
                                licenseKey,
                                "integration-machine-001",
                                "Integration laptop");

                assertThat(repeatedActivationId)
                                .isEqualTo(firstActivationId);

                deactivateMachine(
                                licenseKey,
                                "integration-machine-001");

                String reactivatedId = activateMachine(
                                licenseKey,
                                "integration-machine-001",
                                "Integration laptop");

                assertThat(reactivatedId)
                                .isNotEqualTo(firstActivationId);
        }

        private String createProduct() throws Exception {
                String requestBody = objectMapper.writeValueAsString(
                                Map.of(
                                                "name",
                                                "Integration Product",
                                                "description",
                                                "Full activation flow test"));

                MvcResult result = mockMvc.perform(
                                post("/api/products")
                                                .with(user("manager@example.com")
                                                                .roles("USER"))
                                                .with(csrf())
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode response = objectMapper.readTree(
                                result.getResponse().getContentAsString());

                String productId = response.get("id").asText();

                assertThat(productId).isNotBlank();

                return productId;
        }

        private String issueLicense(String productId)
                        throws Exception {

                String requestBody = objectMapper.writeValueAsString(
                                Map.of(
                                                "customerEmail",
                                                "integration@example.com",
                                                "maxActivations",
                                                1));

                MvcResult result = mockMvc.perform(
                                post(
                                                "/api/products/{productId}/licenses",
                                                productId)
                                                .with(user("manager@example.com")
                                                                .roles("USER"))
                                                .with(csrf())
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode response = objectMapper.readTree(
                                result.getResponse().getContentAsString());

                String licenseKey = response.get("licenseKey").asText();

                assertThat(licenseKey)
                                .startsWith("LH_");

                assertThat(
                                response.get("license")
                                                .get("productId")
                                                .asText())
                                .isEqualTo(productId);

                return licenseKey;
        }

        private String activateMachine(
                        String licenseKey,
                        String fingerprint,
                        String machineName)
                        throws Exception {

                String requestBody = objectMapper.writeValueAsString(
                                Map.of(
                                                "licenseKey",
                                                licenseKey,
                                                "machineFingerprint",
                                                fingerprint,
                                                "machineName",
                                                machineName));

                MvcResult result = mockMvc.perform(
                                post("/api/activations")
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode response = objectMapper.readTree(
                                result.getResponse().getContentAsString());

                String activationId = response.get("id").asText();

                assertThat(activationId).isNotBlank();

                return activationId;
        }

        private void deactivateMachine(
                        String licenseKey,
                        String fingerprint)
                        throws Exception {

                String requestBody = objectMapper.writeValueAsString(
                                Map.of(
                                                "licenseKey",
                                                licenseKey,
                                                "machineFingerprint",
                                                fingerprint));

                mockMvc.perform(
                                post("/api/activations/deactivate")
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isNoContent());
        }
}