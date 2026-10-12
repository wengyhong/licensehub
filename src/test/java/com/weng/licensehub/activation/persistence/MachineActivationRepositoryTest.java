package com.weng.licensehub.activation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MachineActivationRepositoryTest {
        @Autowired
        private UserAccountRepository userAccountRepository;

        private UserAccount owner;

        @BeforeEach
        void setUp() {
                owner = userAccountRepository.save(
                                new UserAccount("owner@example.com", "test-password-hash"));
        }

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private LicenseRepository licenseRepository;

        @Autowired
        private MachineActivationRepository activationRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void findsPagedActivationsForLicenseOnly() {
                Product product = productRepository.save(
                                new Product(owner, "Product", null));

                License targetLicense = licenseRepository.save(
                                new License(
                                                product,
                                                "AAAAAAAAAAAAAAAA",
                                                "a".repeat(64),
                                                "target@example.com",
                                                3,
                                                null));

                License otherLicense = licenseRepository.save(
                                new License(
                                                product,
                                                "BBBBBBBBBBBBBBBB",
                                                "b".repeat(64),
                                                "other@example.com",
                                                3,
                                                null));

                MachineActivation first = activationRepository.save(
                                new MachineActivation(
                                                targetLicense,
                                                "1".repeat(64),
                                                "Laptop"));

                MachineActivation second = activationRepository.save(
                                new MachineActivation(
                                                targetLicense,
                                                "2".repeat(64),
                                                "Desktop"));

                activationRepository.save(
                                new MachineActivation(
                                                otherLicense,
                                                "3".repeat(64),
                                                "Other machine"));

                UUID firstId = first.getId();
                UUID secondId = second.getId();

                entityManager.flush();
                entityManager.clear();

                Pageable pageable = PageRequest.of(
                                0,
                                20,
                                Sort.Direction.DESC,
                                "activatedAt");

                Page<MachineActivation> result = activationRepository.findAllByLicense_Id(
                                targetLicense.getId(),
                                pageable);

                assertThat(result.getContent())
                                .extracting(MachineActivation::getId)
                                .containsExactlyInAnyOrder(firstId, secondId);

                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getTotalPages()).isEqualTo(1);
                assertThat(result.getNumber()).isZero();
                assertThat(result.getSize()).isEqualTo(20);
        }

        @Test
        void findsAndCountsOnlyActiveActivationsForLicense() {
                Product product = productRepository.save(
                                new Product(owner, "Product", null));

                License targetLicense = licenseRepository.save(
                                new License(
                                                product,
                                                "AAAAAAAAAAAAAAAA",
                                                "a".repeat(64),
                                                "target@example.com",
                                                3,
                                                null));

                License otherLicense = licenseRepository.save(
                                new License(
                                                product,
                                                "BBBBBBBBBBBBBBBB",
                                                "b".repeat(64),
                                                "other@example.com",
                                                3,
                                                null));

                String firstFingerprint = "1".repeat(64);
                String secondFingerprint = "2".repeat(64);
                String inactiveFingerprint = "3".repeat(64);

                MachineActivation first = activationRepository.save(
                                new MachineActivation(
                                                targetLicense,
                                                firstFingerprint,
                                                "Laptop"));

                activationRepository.save(
                                new MachineActivation(
                                                targetLicense,
                                                secondFingerprint,
                                                "Desktop"));

                MachineActivation inactive = activationRepository.save(
                                new MachineActivation(
                                                targetLicense,
                                                inactiveFingerprint,
                                                "Old laptop"));

                activationRepository.save(
                                new MachineActivation(
                                                otherLicense,
                                                "4".repeat(64),
                                                "Other machine"));

                entityManager.flush();

                inactive.deactivate(Instant.now());

                entityManager.flush();

                UUID firstId = first.getId();
                UUID targetLicenseId = targetLicense.getId();

                entityManager.clear();

                assertThat(
                                activationRepository
                                                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                                                                targetLicenseId,
                                                                firstFingerprint))
                                .map(MachineActivation::getId)
                                .contains(firstId);

                assertThat(
                                activationRepository
                                                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                                                                targetLicenseId,
                                                                inactiveFingerprint))
                                .isEmpty();

                assertThat(
                                activationRepository
                                                .countByLicense_IdAndDeactivatedAtIsNull(
                                                                targetLicenseId))
                                .isEqualTo(2);
        }

        @Test
        void allowsSameMachineToReactivateAfterDeactivation() {
                Product product = productRepository.save(
                                new Product(owner, "Product", null));

                License license = licenseRepository.save(
                                new License(
                                                product,
                                                "1234567890ABCDEF",
                                                "a".repeat(64),
                                                "customer@example.com",
                                                1,
                                                null));

                String fingerprintHash = "f".repeat(64);

                MachineActivation previous = activationRepository.saveAndFlush(
                                new MachineActivation(
                                                license,
                                                fingerprintHash,
                                                "Development laptop"));

                previous.deactivate(Instant.now());
                entityManager.flush();

                MachineActivation reactivated = activationRepository.saveAndFlush(
                                new MachineActivation(
                                                license,
                                                fingerprintHash,
                                                "Development laptop"));

                UUID reactivatedId = reactivated.getId();
                UUID licenseId = license.getId();

                entityManager.clear();

                MachineActivation active = activationRepository
                                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                                                licenseId,
                                                fingerprintHash)
                                .orElseThrow();

                assertThat(active.getId())
                                .isEqualTo(reactivatedId);

                assertThat(active.getId())
                                .isNotEqualTo(previous.getId());

                assertThat(activationRepository
                                .countByLicense_IdAndDeactivatedAtIsNull(
                                                licenseId))
                                .isEqualTo(1);
        }
}