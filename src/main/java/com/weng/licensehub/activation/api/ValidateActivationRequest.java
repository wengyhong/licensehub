package com.weng.licensehub.activation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateActivationRequest(
    @NotBlank @Size(max = 100) String licenseKey,
    @NotBlank @Size(max = 512) String machineFingerprint) {}
