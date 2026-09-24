package com.weng.licensehub.license.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.weng.licensehub.license.application.IssuedLicense;
import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.domain.LicenseStatus;
import com.weng.licensehub.product.domain.Product;

@WebMvcTest(LicenseController.class)
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
        given(product.getId()).willReturn(productId);
        String keyId = "ABCDEF0123456789";
        String fullKey = "LH_" + keyId + "_test-secret";
        License license = mock(License.class);
        given(license.getId()).willReturn(licenseId);
        given(license.getProduct()).willReturn(product);
        given(license.getKeyId()).willReturn(keyId);
        given(license.getCustomerEmail()).willReturn("customer@example.com");
        given(license.getStatus()).willReturn(LicenseStatus.ACTIVE);
        given(license.getMaxActivations()).willReturn(2);
        given(license.getExpiresAt()).willReturn(expires);
        given(license.getCreatedAt()).willReturn(timestamp);
        given(license.getUpdatedAt()).willReturn(timestamp);
        given(licenseService.issue(
                productId,
                "customer@example.com",
                2,
                expires)).willReturn(new IssuedLicense(license, fullKey));

        mockMvc.perform(post("/api/products/{productId}/licenses", productId)
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
}
