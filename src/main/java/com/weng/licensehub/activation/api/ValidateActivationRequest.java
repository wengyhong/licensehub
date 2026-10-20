package com.weng.licensehub.activation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ValidateActivationRequest(
    @NotNull UUID productId,
    @NotBlank @Size(max = 100) String activationToken,
    @NotBlank @Size(max = 512) String machineFingerprint) {}
