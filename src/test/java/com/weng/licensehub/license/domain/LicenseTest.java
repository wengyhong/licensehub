package com.weng.licensehub.license.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.product.domain.Product;

class LicenseTest {

    private static final Instant NOW =
            Instant.parse("2026-09-28T10:00:00Z");

    @Test
    void activeUnexpiredLicenseCanActivate() {
        License license = createLicense(
                NOW.plusSeconds(60));

        assertThat(license.canActivateAt(NOW)).isTrue();
    }

    @Test
    void licenseCannotActivateAtItsExpiryTime() {
        License license = createLicense(NOW);

        assertThat(license.isExpiredAt(NOW)).isTrue();
        assertThat(license.canActivateAt(NOW)).isFalse();
    }

    @Test
    void licenseWithoutExpiryCanActivate() {
        License license = createLicense(null);

        assertThat(license.isExpiredAt(NOW)).isFalse();
        assertThat(license.canActivateAt(NOW)).isTrue();
    }

    private License createLicense(Instant expiresAt) {
        return new License(
                new Product("Product", null),
                "0123456789ABCDEF",
                "a".repeat(64),
                "customer@example.com",
                1,
                expiresAt);
    }
}