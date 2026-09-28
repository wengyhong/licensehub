package com.weng.licensehub.license.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LicenseRepositoryTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private LicenseRepository licenseRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void savesAndLoadsLicenseWithItsProduct() {
                Product product = productRepository.save(
                                new Product("LicenseHub", "Test product"));

                License license = new License(
                                product,
                                "0123456789ABCDEF",
                                "a".repeat(64),
                                "customer@example.com",
                                2,
                                null);

                License saved = licenseRepository.save(license);

                entityManager.flush();
                entityManager.clear();

                License loaded = licenseRepository.findById(saved.getId())
                                .orElseThrow();

                assertThat(loaded.getCustomerEmail())
                                .isEqualTo("customer@example.com");

                assertThat(loaded.getProduct().getId())
                                .isEqualTo(product.getId());
        }

        @Test
        void findsLicensesForProductNewestFirst() {
                Product targetProduct = productRepository.save(
                                new Product("Target product", null));

                Product otherProduct = productRepository.save(
                                new Product("Other product", null));

                License olderLicense = licenseRepository.saveAndFlush(
                                new License(
                                                targetProduct,
                                                "AAAAAAAAAAAAAAAA",
                                                "a".repeat(64),
                                                "older@example.com",
                                                1,
                                                null));

                License newerLicense = licenseRepository.saveAndFlush(
                                new License(
                                                targetProduct,
                                                "BBBBBBBBBBBBBBBB",
                                                "b".repeat(64),
                                                "newer@example.com",
                                                1,
                                                null));

                licenseRepository.saveAndFlush(
                                new License(
                                                otherProduct,
                                                "CCCCCCCCCCCCCCCC",
                                                "c".repeat(64),
                                                "other@example.com",
                                                1,
                                                null));

                entityManager.clear();

                List<License> result = licenseRepository
                                .findAllByProduct_IdOrderByCreatedAtDesc(
                                                targetProduct.getId());

                assertThat(result)
                                .extracting(License::getKeyId)
                                .containsExactly(
                                                newerLicense.getKeyId(),
                                                olderLicense.getKeyId());
        }

        @Test
        void findsLicenseByKeyId() {
                // Arrange: save a product and a license with a known keyId
                Product targetProduct = productRepository.save(
                                new Product("Target product", null));

                String keyId = "0123456789ABCDEF";

                License license = licenseRepository.saveAndFlush(new License(
                                targetProduct,
                                keyId,
                                "a".repeat(64),
                                "customer@example.com",
                                2,
                                null));

                entityManager.clear();

                // Flush and clear the persistence context

                // Act: call licenseRepository.findByKeyId(...)
                Optional<License> result = licenseRepository.findByKeyId(keyId);
                assertThat(result).isPresent();

                License loaded = result.orElseThrow();

                assertThat(loaded.getId()).isEqualTo(license.getId());
                assertThat(loaded.getKeyId()).isEqualTo(keyId);

                // Assert: the Optional is present and contains the expected license
        }

        @Test
        void findsLicenseWithPessimisticWriteLock() {
                Product product = productRepository.save(
                                new Product("Product", null));

                License saved = licenseRepository.saveAndFlush(
                                new License(
                                                product,
                                                "FEDCBA9876543210",
                                                "a".repeat(64),
                                                "customer@example.com",
                                                2,
                                                null));

                UUID licenseId = saved.getId();

                entityManager.clear();

                License locked = licenseRepository
                                .findByIdForUpdate(licenseId)
                                .orElseThrow();

                assertThat(locked.getId()).isEqualTo(licenseId);

                assertThat(entityManager.getLockMode(locked))
                                .isEqualTo(
                                                LockModeType.PESSIMISTIC_WRITE);
        }
}