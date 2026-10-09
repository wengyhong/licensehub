package com.weng.licensehub.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductOwnershipIntegrationTest {

        private static final String OWNER_EMAIL = "owner@example.com";
        private static final String OTHER_OWNER_EMAIL = "other-owner@example.com";

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private ProductRepository productRepository;
        @Autowired
        private LicenseRepository licenseRepository;
        @Autowired
        private UserAccountRepository userAccountRepository;

        @Test
        void scopesProductReadsToAuthenticatedOwner() throws Exception {
                userAccountRepository.save(
                                new UserAccount(OWNER_EMAIL, "{noop}unused"));
                userAccountRepository.save(
                                new UserAccount(OTHER_OWNER_EMAIL, "{noop}unused"));

                String ownerProductId = createProduct(
                                OWNER_EMAIL,
                                "Owner Product");
                createProduct(
                                OTHER_OWNER_EMAIL,
                                "Other Owner Product");

                mockMvc.perform(get("/api/products")
                                .with(user(OWNER_EMAIL).roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()")
                                                .value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(ownerProductId))
                                .andExpect(jsonPath("$.content[0].name")
                                                .value("Owner Product"))
                                .andExpect(jsonPath("$.totalElements")
                                                .value(1))
                                .andExpect(jsonPath("$.number")
                                                .value(0))
                                .andExpect(jsonPath("$.size")
                                                .value(20));

                mockMvc.perform(get("/api/products/{id}", ownerProductId)
                                .with(user(OTHER_OWNER_EMAIL).roles("USER")))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title").value("Product not found"));

                mockMvc.perform(get("/api/products/{id}", ownerProductId)
                                .with(user(OWNER_EMAIL).roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(ownerProductId));
        }

        private String createProduct(
                        String ownerEmail,
                        String productName) throws Exception {

                MvcResult result = mockMvc.perform(post("/api/products")
                                .with(user(ownerEmail).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "%s",
                                                  "description": "Ownership integration test"
                                                }
                                                """.formatted(productName)))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode response = objectMapper.readTree(
                                result.getResponse().getContentAsString());

                return response.get("id").asText();
        }

        @Test
        void onlyOwnerCanUpdateProduct() throws Exception {
                String updateOwnerEmail = "update-owner@example.com";

                String otherUserEmail = "update-other@example.com";

                userAccountRepository.save(
                                new UserAccount(
                                                updateOwnerEmail,
                                                "{noop}unused"));

                userAccountRepository.save(
                                new UserAccount(
                                                otherUserEmail,
                                                "{noop}unused"));

                String productId = createProduct(
                                updateOwnerEmail,
                                "Original Product");

                String requestBody = """
                                {
                                  "name": "Updated Product",
                                  "description": "Updated description"
                                }
                                """;

                // Another user cannot update it.
                mockMvc.perform(put(
                                "/api/products/{productId}",
                                productId)
                                .with(user(otherUserEmail).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isNotFound());

                // Its owner can update it.
                mockMvc.perform(put(
                                "/api/products/{productId}",
                                productId)
                                .with(user(updateOwnerEmail).roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name")
                                                .value("Updated Product"))
                                .andExpect(jsonPath("$.description")
                                                .value("Updated description"));

                // A later request reads the persisted values.
                mockMvc.perform(get(
                                "/api/products/{productId}",
                                productId)
                                .with(user(updateOwnerEmail).roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name")
                                                .value("Updated Product"))
                                .andExpect(jsonPath("$.description")
                                                .value("Updated description"));
        }

        @Test
        void onlyOwnerCanDeleteProductWithoutLicenses()
                        throws Exception {

                String deleteOwnerEmail = "delete-owner@example.com";

                String otherUserEmail = "delete-other@example.com";

                userAccountRepository.save(
                                new UserAccount(
                                                deleteOwnerEmail,
                                                "{noop}unused"));

                userAccountRepository.save(
                                new UserAccount(
                                                otherUserEmail,
                                                "{noop}unused"));

                String productId = createProduct(
                                deleteOwnerEmail,
                                "Deletable Product");

                // Another user cannot delete it.
                mockMvc.perform(delete(
                                "/api/products/{productId}",
                                productId)
                                .with(user(otherUserEmail).roles("USER"))
                                .with(csrf()))
                                .andExpect(status().isNotFound());

                // The product still exists.
                mockMvc.perform(get(
                                "/api/products/{productId}",
                                productId)
                                .with(user(deleteOwnerEmail).roles("USER")))
                                .andExpect(status().isOk());

                // Its owner can delete it.
                mockMvc.perform(delete(
                                "/api/products/{productId}",
                                productId)
                                .with(user(deleteOwnerEmail).roles("USER"))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                // A later request confirms deletion persisted.
                mockMvc.perform(get(
                                "/api/products/{productId}",
                                productId)
                                .with(user(deleteOwnerEmail).roles("USER")))
                                .andExpect(status().isNotFound());
        }

        @Test
        void cannotDeleteProductWithLicenses()
                        throws Exception {

                String ownerEmail = "licensed-product-owner@example.com";

                userAccountRepository.save(
                                new UserAccount(
                                                ownerEmail,
                                                "{noop}unused"));

                String productId = createProduct(
                                ownerEmail,
                                "Licensed Product");

                Product product = productRepository.findById(
                                UUID.fromString(productId))
                                .orElseThrow();

                licenseRepository.saveAndFlush(
                                new License(
                                                product,
                                                "EEEEEEEEEEEEEEEE",
                                                "e".repeat(64),
                                                "customer@example.com",
                                                1,
                                                null));

                mockMvc.perform(delete(
                                "/api/products/{productId}",
                                productId)
                                .with(user(ownerEmail).roles("USER"))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.title")
                                                .value("Product has existing licenses"));

                // The product remains available.
                mockMvc.perform(get(
                                "/api/products/{productId}",
                                productId)
                                .with(user(ownerEmail).roles("USER")))
                                .andExpect(status().isOk());
        }
}
