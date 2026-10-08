package com.weng.licensehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.domain.LicenseStatus;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LicenseOwnershipIntegrationTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserAccountRepository userAccountRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private LicenseRepository licenseRepository;

        @Test
        void preventsAccessToAnotherOwnersLicenses()
                        throws Exception {

                UserAccount alice = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "alice@example.com",
                                                "{noop}unused"));

                UserAccount bob = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "bob@example.com",
                                                "{noop}unused"));

                Product aliceProduct = productRepository.saveAndFlush(
                                new Product(
                                                alice,
                                                "Alice Product",
                                                null));

                License aliceLicense = licenseRepository.saveAndFlush(
                                new License(
                                                aliceProduct,
                                                "AAAAAAAAAAAAAAAA",
                                                "a".repeat(64),
                                                "customer@example.com",
                                                2,
                                                null));

                mockMvc.perform(get(
                                "/api/licenses/{licenseId}",
                                aliceLicense.getId())
                                .with(user(alice.getEmail())
                                                .roles("USER")))
                                .andExpect(status().isOk());

                mockMvc.perform(get(
                                "/api/licenses/{licenseId}",
                                aliceLicense.getId())
                                .with(user(bob.getEmail())
                                                .roles("USER")))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title")
                                                .value("License not found"));

                mockMvc.perform(get(
                                "/api/products/{productId}/licenses",
                                aliceProduct.getId())
                                .with(user(bob.getEmail())
                                                .roles("USER")))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title")
                                                .value("Product not found"));

                long licenseCountBefore = licenseRepository.count();

                mockMvc.perform(post(
                                "/api/products/{productId}/licenses",
                                aliceProduct.getId())
                                .with(user(bob.getEmail())
                                                .roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "customerEmail": "other@example.com",
                                                  "maxActivations": 1
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title")
                                                .value("Product not found"));

                assertThat(licenseRepository.count())
                                .isEqualTo(licenseCountBefore);
        }

        @Test
        void onlyOwnerCanRevokeLicense() throws Exception {
                UserAccount owner = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "revocation-owner@example.com",
                                                "{noop}unused"));

                UserAccount otherUser = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "revocation-other@example.com",
                                                "{noop}unused"));

                Product product = productRepository.saveAndFlush(
                                new Product(owner, "Revocation Product", null));

                License license = licenseRepository.saveAndFlush(
                                new License(
                                                product,
                                                "BBBBBBBBBBBBBBBB",
                                                "b".repeat(64),
                                                "customer@example.com",
                                                1,
                                                null));

                // Another user cannot revoke it.
                mockMvc.perform(post(
                                "/api/licenses/{licenseId}/revoke",
                                license.getId())
                                .with(user(otherUser.getEmail()).roles("USER"))
                                .with(csrf()))
                                .andExpect(status().isNotFound());

                assertThat(licenseRepository.findById(license.getId())
                                .orElseThrow()
                                .getStatus())
                                .isEqualTo(LicenseStatus.ACTIVE);

                // Its owner can revoke it.
                mockMvc.perform(post(
                                "/api/licenses/{licenseId}/revoke",
                                license.getId())
                                .with(user(owner.getEmail()).roles("USER"))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                assertThat(licenseRepository.findById(license.getId())
                                .orElseThrow()
                                .getStatus())
                                .isEqualTo(LicenseStatus.REVOKED);
        }
}