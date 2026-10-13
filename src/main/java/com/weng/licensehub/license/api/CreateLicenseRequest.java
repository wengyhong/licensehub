package com.weng.licensehub.license.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateLicenseRequest(
    @NotBlank @Email @Size(max = 320) String customerEmail,
    @NotNull @Min(1) Integer maxActivations,
    @Future Instant expiresAt) {}
