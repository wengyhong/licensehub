package com.weng.licensehub.license.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

class LicenseServiceTest {

        private static final String OWNER_EMAIL = "owner@example.com";

        private LicenseRepository licenseRepository;
        private ProductRepository productRepository;
        private LicenseKeyGenerator licenseKeyGenerator;
        private UserAccountRepository userAccountRepository;
        private LicenseService service;

        @BeforeEach
        void setUp() {
                licenseRepository = mock(LicenseRepository.class);
                productRepository = mock(ProductRepository.class);
                licenseKeyGenerator = mock(LicenseKeyGenerator.class);
                userAccountRepository = mock(UserAccountRepository.class);

                service = new LicenseService(
                                licenseRepository,
                                productRepository,
                                licenseKeyGenerator,
                                userAccountRepository);
        }

        @Test
        void issueForOwnerSavesHashAndReturnsFullKey() {
                UUID ownerId = stubOwner();
                UUID productId = UUID.randomUUID();
                Product product = mock(Product.class);
                Instant expiresAt = Instant.parse("2027-09-23T10:00:00Z");

                String keyId = "ABCDEF0123456789";
                String keyHash = "a".repeat(64);
                String fullKey = "LH_" + keyId + "_secret";

                GeneratedLicenseKey generatedKey = new GeneratedLicenseKey(
                                keyId,
                                keyHash,
                                fullKey);

                when(productRepository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.of(product));

                when(licenseKeyGenerator.generate())
                                .thenReturn(generatedKey);

                when(licenseRepository.save(any(License.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                IssuedLicense result = service.issueForOwner(
                                productId,
                                OWNER_EMAIL,
                                "  customer@example.com  ",
                                2,
                                expiresAt);

                License saved = result.license();

                assertSame(product, saved.getProduct());
                assertEquals(keyId, saved.getKeyId());
                assertEquals(keyHash, saved.getKeyHash());
                assertEquals(
                                "customer@example.com",
                                saved.getCustomerEmail());
                assertEquals(2, saved.getMaxActivations());
                assertEquals(expiresAt, saved.getExpiresAt());
                assertEquals(
                                LicenseStatus.ACTIVE,
                                saved.getStatus());
                assertEquals(fullKey, result.fullKey());

                verify(licenseRepository).save(saved);
        }

        @Test
        void issueForOwnerThrowsWhenProductIsNotOwned() {
                UUID ownerId = stubOwner();
                UUID productId = UUID.randomUUID();

                when(productRepository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.empty());

                ProductNotFoundException exception = assertThrows(
                                ProductNotFoundException.class,
                                () -> service.issueForOwner(
                                                productId,
                                                OWNER_EMAIL,
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
        void ownerRevokeLicense() {
                UUID licenseId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);
                License license = mock(License.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);

                when(licenseRepository.findByIdAndProduct_Owner_Id(
                                licenseId,
                                ownerId))
                                .thenReturn(Optional.of(license));
        }

        @Test
        void anotherOwnerRevokeLicense() {
                UUID licenseId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                String ownerEmail = "owner@example.com";

                UserAccount owner = mock(UserAccount.class);
                License license = mock(License.class);

                when(userAccountRepository.findByEmailIgnoreCase(ownerEmail))
                                .thenReturn(Optional.of(owner));

                when(owner.getId()).thenReturn(ownerId);
                when(licenseRepository.findByIdAndProduct_Owner_Id(
                                licenseId,
                                ownerId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                LicenseNotFoundException.class,
                                () -> service.revokeForOwner(
                                                licenseId,
                                                ownerEmail));

                verifyNoInteractions(license);
        }

        @Test
        void getByIdForOwnerReturnsRepositoryResult() {
                UUID ownerId = stubOwner();
                UUID licenseId = UUID.randomUUID();
                License license = mock(License.class);

                when(licenseRepository.findByIdAndProduct_Owner_Id(
                                licenseId,
                                ownerId))
                                .thenReturn(Optional.of(license));

                License result = service.getByIdForOwner(
                                licenseId,
                                OWNER_EMAIL);

                assertSame(license, result);
        }


        @Test
        void getByIdForOwnerThrowsWhenLicenseIsNotOwned() {
                UUID ownerId = stubOwner();
                UUID licenseId = UUID.randomUUID();

                when(licenseRepository.findByIdAndProduct_Owner_Id(
                                licenseId,
                                ownerId))
                                .thenReturn(Optional.empty());

                LicenseNotFoundException exception = assertThrows(
                                LicenseNotFoundException.class,
                                () -> service.getByIdForOwner(
                                                licenseId,
                                                OWNER_EMAIL));

                assertEquals(
                                "License '" + licenseId + "' not found",
                                exception.getMessage());
        }

        @Test
        void findAllByProductIdForOwnerReturnsRepositoryResults() {
                UUID ownerId = stubOwner();
                UUID productId = UUID.randomUUID();
                Product product = mock(Product.class);
                License license = mock(License.class);
                List<License> expected = List.of(license);

                when(productRepository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.of(product));

                when(licenseRepository
                                .findAllByProduct_IdOrderByCreatedAtDesc(
                                                productId))
                                .thenReturn(expected);

                List<License> result = service.findAllByProductIdForOwner(
                                productId,
                                OWNER_EMAIL);

                assertEquals(expected, result);

                verify(licenseRepository)
                                .findAllByProduct_IdOrderByCreatedAtDesc(
                                                productId);
        }

        @Test
        void findAllByProductIdForOwnerThrowsWhenProductIsNotOwned() {
                UUID ownerId = stubOwner();
                UUID productId = UUID.randomUUID();

                when(productRepository.findByIdAndOwner_Id(
                                productId,
                                ownerId))
                                .thenReturn(Optional.empty());

                ProductNotFoundException exception = assertThrows(
                                ProductNotFoundException.class,
                                () -> service.findAllByProductIdForOwner(
                                                productId,
                                                OWNER_EMAIL));

                assertEquals(
                                "Product '" + productId + "' was not found",
                                exception.getMessage());

                verifyNoInteractions(licenseRepository);
        }

        private UUID stubOwner() {
                UUID ownerId = UUID.randomUUID();
                UserAccount owner = mock(UserAccount.class);

                when(owner.getId()).thenReturn(ownerId);

                when(userAccountRepository.findByEmailIgnoreCase(
                                OWNER_EMAIL))
                                .thenReturn(Optional.of(owner));

                return ownerId;
        }
}
