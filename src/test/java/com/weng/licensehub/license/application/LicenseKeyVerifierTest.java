package com.weng.licensehub.license.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.shared.security.Sha256Hasher;

@ExtendWith(MockitoExtension.class)
class LicenseKeyVerifierTest {

        @Mock
        private LicenseRepository licenseRepository;

        private final LicenseKeyParser parser = new LicenseKeyParser();
        private final Sha256Hasher hasher = new Sha256Hasher();

        private LicenseKeyVerifier verifier;

        @BeforeEach
        void setup() {
                verifier = new LicenseKeyVerifier(
                                hasher,
                                parser,
                                licenseRepository);
        }

        @Test
        void returnsLicenseWhenKeyIsValid() {

                String keyId = "0123456789ABCDEF";
                String secret = "A".repeat(43);
                String fullKey = "LH_" + keyId + "_" + secret;

                License license = new License(
                                new Product("Product", null),
                                keyId,
                                hasher.hash(secret),
                                "customer@example.com",
                                1,
                                null);

                when(licenseRepository.findByKeyId(keyId))
                                .thenReturn(Optional.of(license));

                License result = verifier.verify(fullKey);

                assertThat(result).isSameAs(license);
        }

        @Test
        void rejectsMalformedKeyWithoutDatabaseLookup() {
                assertThatThrownBy(
                                () -> verifier.verify("not-a-license-key"))
                                .isInstanceOf(InvalidLicenseKeyException.class)
                                .hasMessage("Invalid license key");

                verifyNoInteractions(licenseRepository);
        }

        @Test
        void rejectsUnknownKeyId() {
                String keyId = "0123456789ABCDEF";
                String fullKey = "LH_" + keyId + "_" + "A".repeat(43);

                when(licenseRepository.findByKeyId(keyId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> verifier.verify(fullKey))
                                .isInstanceOf(InvalidLicenseKeyException.class)
                                .hasMessage("Invalid license key");
        }

        @Test
        void rejectsIncorrectSecret() {
                String keyId = "0123456789ABCDEF";
                String correctSecret = "A".repeat(43);
                String incorrectSecret = "B".repeat(43);

                License license = new License(
                                new Product("Product", null),
                                keyId,
                                hasher.hash(correctSecret),
                                "customer@example.com",
                                1,
                                null);

                when(licenseRepository.findByKeyId(keyId))
                                .thenReturn(Optional.of(license));

                String suppliedKey = "LH_" + keyId + "_" + incorrectSecret;

                assertThatThrownBy(() -> verifier.verify(suppliedKey))
                                .isInstanceOf(InvalidLicenseKeyException.class)
                                .hasMessage("Invalid license key");
        }
}