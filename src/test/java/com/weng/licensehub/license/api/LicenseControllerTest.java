package com.weng.licensehub.license.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.license.application.IssuedLicense;
import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.domain.LicenseStatus;
import com.weng.licensehub.product.domain.Product;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;

import com.weng.licensehub.product.application.ProductNotFoundException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@WebMvcTest(LicenseController.class)
@AutoConfigureMockMvc(addFilters = false)

class LicenseControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private LicenseService licenseService;

        @Test
        void issueReturns201AndFullKey() throws Exception {
                UUID productId = UUID.fromString("7a38cd7d-e02b-4ed7-bb88-31f284d85a34");

                UUID licenseId = UUID.fromString("b055a27c-e830-47fe-bcc4-748d26875a4d");
                Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

                Instant expires = Instant.parse("2027-09-23T10:00:00Z");
                Product product = mock(Product.class);
                when(product.getId()).thenReturn(productId);
                String keyId = "ABCDEF0123456789";
                String fullKey = "LH_" + keyId + "_test-secret";
                License license = mock(License.class);
                when(license.getId()).thenReturn(licenseId);
                when(license.getProduct()).thenReturn(product);
                when(license.getKeyId()).thenReturn(keyId);
                when(license.getCustomerEmail()).thenReturn("customer@example.com");
                when(license.getStatus()).thenReturn(LicenseStatus.ACTIVE);
                when(license.getMaxActivations()).thenReturn(2);
                when(license.getExpiresAt()).thenReturn(expires);
                when(license.getCreatedAt()).thenReturn(timestamp);
                when(license.getUpdatedAt()).thenReturn(timestamp);

                when(licenseService.issueForOwner(
                                productId,
                                "owner@example.com",
                                "customer@example.com",
                                2,
                                expires))
                                .thenReturn(new IssuedLicense(
                                                license,
                                                fullKey));

                mockMvc.perform(post("/api/products/{productId}/licenses", productId)
                                .principal(() -> "owner@example.com")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                    {
                                                        "customerEmail": "customer@example.com",
                                                        "maxActivations": 2,
                                                        "expiresAt": "2027-09-23T10:00:00Z"
                                                    }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.license.id").value(licenseId.toString()))
                                .andExpect(jsonPath("$.license.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.license.keyId").value(keyId))
                                .andExpect(jsonPath("$.license.status").value(LicenseStatus.ACTIVE.toString()))
                                .andExpect(jsonPath("$.license.maxActivations").value(2))
                                .andExpect(jsonPath("$.licenseKey").value(fullKey))
                                .andExpect(jsonPath("$.license.keyHash").doesNotExist())
                                .andExpect(jsonPath("$.license.customerEmail")
                                                .value("customer@example.com"))
                                .andExpect(jsonPath("$.license.expiresAt")
                                                .value(expires.toString()))
                                .andExpect(jsonPath("$.license.createdAt")
                                                .value(timestamp.toString()))
                                .andExpect(jsonPath("$.license.updatedAt")
                                                .value(timestamp.toString()));
        }

        @Test
        void getByIdReturns200WhenLicenseExists() throws Exception {
                UUID productId = UUID.randomUUID();
                UUID licenseId = UUID.randomUUID();
                Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(productId);

                License license = mock(License.class);
                when(license.getId()).thenReturn(licenseId);
                when(license.getProduct()).thenReturn(product);
                when(license.getKeyId()).thenReturn("ABCDEF0123456789");
                when(license.getCustomerEmail())
                                .thenReturn("customer@example.com");
                when(license.getStatus()).thenReturn(LicenseStatus.ACTIVE);
                when(license.getMaxActivations()).thenReturn(2);
                when(license.getCreatedAt()).thenReturn(timestamp);
                when(license.getUpdatedAt()).thenReturn(timestamp);

                when(licenseService.getByIdForOwner(
                                licenseId,
                                "owner@example.com"))
                                .thenReturn(license);

                mockMvc.perform(get("/api/licenses/{licenseId}", licenseId).principal(() -> "owner@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(licenseId.toString()))
                                .andExpect(jsonPath("$.productId").value(productId.toString()))
                                .andExpect(jsonPath("$.keyId")
                                                .value("ABCDEF0123456789"))
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.keyHash").doesNotExist())
                                .andExpect(jsonPath("$.licenseKey").doesNotExist());
        }

        @Test
        void getByIdReturns404WhenLicenseDoesNotExist() throws Exception {
                UUID licenseId = UUID.randomUUID();

                when(licenseService.getByIdForOwner(
                                licenseId,
                                "owner@example.com"))
                                .thenThrow(new LicenseNotFoundException(
                                                licenseId));

                mockMvc.perform(get("/api/licenses/{licenseId}", licenseId).principal(() -> "owner@example.com"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title")
                                                .value("License not found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("License '" + licenseId + "' not found"))
                                .andExpect(jsonPath("$.instance")
                                                .value("/api/licenses/" + licenseId));
        }

        @Test
        void findAllByProductIdReturns200AndLicenses() throws Exception {
                UUID productId = UUID.randomUUID();
                UUID licenseId = UUID.randomUUID();

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(productId);

                License license = mock(License.class);
                when(license.getId()).thenReturn(licenseId);
                when(license.getProduct()).thenReturn(product);
                when(license.getKeyId()).thenReturn("ABCDEF0123456789");
                when(license.getCustomerEmail())
                                .thenReturn("customer@example.com");
                when(license.getStatus()).thenReturn(LicenseStatus.ACTIVE);
                when(license.getMaxActivations()).thenReturn(2);

                Pageable pageable = PageRequest.of(
                                0,
                                20,
                                Sort.Direction.DESC,
                                "createdAt");

                Page<License> page = new PageImpl<>(
                                List.of(license),
                                pageable,
                                1);

                when(licenseService.findAllByProductIdForOwner(
                                productId,
                                "owner@example.com",
                                pageable))
                                .thenReturn(page);
                mockMvc.perform(
                                get("/api/products/{productId}/licenses", productId)
                                                .principal(() -> "owner@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(licenseId.toString()))
                                .andExpect(jsonPath("$.content[0].productId")
                                                .value(productId.toString()))
                                .andExpect(jsonPath("$.content[0].keyHash")
                                                .doesNotExist())
                                .andExpect(jsonPath("$.content[0].licenseKey")
                                                .doesNotExist())
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.number").value(0))
                                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        void findAllByProductIdReturns404WhenProductDoesNotExist()
                        throws Exception {

                UUID productId = UUID.randomUUID();

                Pageable pageable = PageRequest.of(
                                0,
                                20,
                                Sort.Direction.DESC,
                                "createdAt");

                when(licenseService.findAllByProductIdForOwner(
                                productId,
                                "owner@example.com",
                                pageable))
                                .thenThrow(
                                                new ProductNotFoundException(productId));

                mockMvc.perform(get(
                                "/api/products/{productId}/licenses",
                                productId)
                                .principal(
                                                () -> "owner@example.com"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title")
                                                .value("Product not found"))
                                .andExpect(jsonPath("$.status")
                                                .value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Product '" + productId
                                                                + "' was not found"));
        }

        @Test
        void issueReturns400WhenRequestIsInvalid() throws Exception {
                UUID productId = UUID.randomUUID();

                mockMvc.perform(
                                post("/api/products/{productId}/licenses", productId)
                                                .principal(() -> "owner@example.com")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "customerEmail": " ",
                                                                  "maxActivations": 0,
                                                                  "expiresAt": "2000-01-01T00:00:00Z"
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(licenseService);
        }

        @Test
        void revokeReturns204() throws Exception {
                UUID licenseId = UUID.randomUUID();

                mockMvc.perform(post(
                                "/api/licenses/{licenseId}/revoke",
                                licenseId)
                                .principal(() -> "owner@example.com")
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(licenseService).revokeForOwner(
                                licenseId,
                                "owner@example.com");
        }

        @Test
        void revokeReturns404WhenLicenseDoesNotExist() throws Exception {
                UUID licenseId = UUID.randomUUID();

                doThrow(new LicenseNotFoundException(licenseId))
                                .when(licenseService)
                                .revokeForOwner(
                                                licenseId,
                                                "owner@example.com");

                mockMvc.perform(post(
                                "/api/licenses/{licenseId}/revoke",
                                licenseId)
                                .principal(() -> "owner@example.com")
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

}
