package com.weng.licensehub.integration;

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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductOwnershipIntegrationTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_OWNER_EMAIL = "other-owner@example.com";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

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
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ownerProductId))
                .andExpect(jsonPath("$[0].name").value("Owner Product"));

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
}
