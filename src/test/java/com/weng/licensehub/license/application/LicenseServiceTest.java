package com.weng.licensehub.license.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.domain.LicenseStatus;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.application.ProductNotFoundException;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;

class LicenseServiceTest {

        private LicenseRepository licenseRepository;
        private ProductRepository productRepository;
        private LicenseKeyGenerator licenseKeyGenerator;
        private LicenseService service;

        @BeforeEach
        void setUp() {
                licenseRepository = mock(LicenseRepository.class);
                productRepository = mock(ProductRepository.class);
                licenseKeyGenerator = mock(LicenseKeyGenerator.class);

                service = new LicenseService(
                                licenseRepository,
                                productRepository,
                                licenseKeyGenerator);
        }

        @Test
        void issueSavesHashAndReturnsFullKey() {
                UUID productId = UUID.randomUUID();
                Product product = mock(Product.class);
                Instant expiresAt = Instant.parse("2027-09-23T10:00:00Z");

                String keyId = "ABCDEF0123456789";
                String keyHash = "a".repeat(64);
                String fullKey = "LH_" + keyId + "_secret";

                GeneratedLicenseKey generatedKey = new GeneratedLicenseKey(keyId, keyHash, fullKey);

                given(productRepository.findById(productId))
                                .willReturn(Optional.of(product));

                given(licenseKeyGenerator.generate())
                                .willReturn(generatedKey);

                given(licenseRepository.save(any(License.class)))
                                .willAnswer(invocation -> invocation.getArgument(0));

                IssuedLicense result = service.issue(
                                productId,
                                "  customer@example.com  ",
                                2,
                                expiresAt);

                License saved = result.license();

                assertSame(product, saved.getProduct());
                assertEquals(keyId, saved.getKeyId());
                assertEquals(keyHash, saved.getKeyHash());
                assertEquals("customer@example.com", saved.getCustomerEmail());
                assertEquals(2, saved.getMaxActivations());
                assertEquals(expiresAt, saved.getExpiresAt());
                assertEquals(LicenseStatus.ACTIVE, saved.getStatus());
                assertEquals(fullKey, result.fullKey());

                verify(licenseRepository).save(saved);
        }

        @Test
        void issueThrowsWhenProductDoesNotExist() {
                UUID productId = UUID.randomUUID();

                given(productRepository.findById(productId))
                                .willReturn(Optional.empty());

                ProductNotFoundException exception = assertThrows(
                                ProductNotFoundException.class,
                                () -> service.issue(
                                                productId,
                                                "customer@example.com",
                                                2,
                                                null));

                assertEquals(
                                "Product '" + productId + "' was not found",
                                exception.getMessage());

                verifyNoInteractions(
                                licenseKeyGenerator,
                                licenseRepository);
        }

        @Test
        void getByIdThrowsWhenLicenseDoesNotExist() {
                UUID licenseId = UUID.randomUUID();

                given(licenseRepository.findById(licenseId))
                                .willReturn(Optional.empty());

                LicenseNotFoundException exception = assertThrows(
                                LicenseNotFoundException.class,
                                () -> service.getById(licenseId));

                assertEquals(
                                "License '" + licenseId + "' not found",
                                exception.getMessage());
        }

        @Test
        void findAllByProductIdReturnsRepositoryResults() {
                UUID productId = UUID.randomUUID();
                License license = mock(License.class);
                List<License> expected = List.of(license);

                given(productRepository.existsById(productId))
                                .willReturn(true);

                given(
                                licenseRepository
                                                .findAllByProduct_IdOrderByCreatedAtDesc(productId))
                                .willReturn(expected);

                List<License> result = service.findAllByProductId(productId);

                assertEquals(expected, result);

                verify(licenseRepository)
                                .findAllByProduct_IdOrderByCreatedAtDesc(productId);
        }

        @Test
        void findAllByProductIdThrowsWhenProductDoesNotExist() {
                UUID productId = UUID.randomUUID();

                given(productRepository.existsById(productId))
                                .willReturn(false);

                ProductNotFoundException exception = assertThrows(
                                ProductNotFoundException.class,
                                () -> service.findAllByProductId(productId));

                assertEquals(
                                "Product '" + productId + "' was not found",
                                exception.getMessage());

                verifyNoInteractions(licenseRepository);
        }
}
