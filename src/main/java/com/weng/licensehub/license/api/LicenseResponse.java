package com.weng.licensehub.license.api;

import java.time.Instant;
import java.util.UUID;

import com.weng.licensehub.license.domain.LicenseStatus;

public record LicenseResponse(

        UUID id,
        UUID productId,
        String keyId,
        String customerEmail,
        LicenseStatus status,
        int maxActivations,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {

}
