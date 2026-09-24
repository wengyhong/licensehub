package com.weng.licensehub.license.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.domain.LicenseStatus;
import com.weng.licensehub.product.domain.Product;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verifyNoInteractions;
import com.weng.licensehub.license.application.LicenseNotFoundException;

import java.util.List;

import com.weng.licensehub.product.application.ProductNotFoundException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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

    @Test
    void getByIdReturns200WhenLicenseExists() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID licenseId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-09-23T10:00:00Z");

        Product product = mock(Product.class);
        given(product.getId()).willReturn(productId);

        License license = mock(License.class);
        given(license.getId()).willReturn(licenseId);
        given(license.getProduct()).willReturn(product);
        given(license.getKeyId()).willReturn("ABCDEF0123456789");
        given(license.getCustomerEmail())
                .willReturn("customer@example.com");
        given(license.getStatus()).willReturn(LicenseStatus.ACTIVE);
        given(license.getMaxActivations()).willReturn(2);
        given(license.getCreatedAt()).willReturn(timestamp);
        given(license.getUpdatedAt()).willReturn(timestamp);

        given(licenseService.getById(licenseId))
                .willReturn(license);

        mockMvc.perform(get("/api/licenses/{licenseId}", licenseId))
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

        given(licenseService.getById(licenseId))
                .willThrow(new LicenseNotFoundException(licenseId));

        mockMvc.perform(get("/api/licenses/{licenseId}", licenseId))
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
        given(product.getId()).willReturn(productId);

        License license = mock(License.class);
        given(license.getId()).willReturn(licenseId);
        given(license.getProduct()).willReturn(product);
        given(license.getKeyId()).willReturn("ABCDEF0123456789");
        given(license.getCustomerEmail())
                .willReturn("customer@example.com");
        given(license.getStatus()).willReturn(LicenseStatus.ACTIVE);
        given(license.getMaxActivations()).willReturn(2);

        given(licenseService.findAllByProductId(productId))
                .willReturn(List.of(license));

        mockMvc.perform(
                get("/api/products/{productId}/licenses", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(licenseId.toString()))
                .andExpect(jsonPath("$[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$[0].keyHash").doesNotExist())
                .andExpect(jsonPath("$[0].licenseKey").doesNotExist());
    }

    @Test
    void findAllByProductIdReturns404WhenProductDoesNotExist()
            throws Exception {

        UUID productId = UUID.randomUUID();

        given(licenseService.findAllByProductId(productId))
                .willThrow(new ProductNotFoundException(productId));

        mockMvc.perform(
                get("/api/products/{productId}/licenses", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Product '" + productId + "' was not found"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/products/" + productId + "/licenses"));
    }

    @Test
    void issueReturns400WhenRequestIsInvalid() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(
                post("/api/products/{productId}/licenses", productId)
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


}
