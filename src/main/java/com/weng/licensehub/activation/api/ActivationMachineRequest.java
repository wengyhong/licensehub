package com.weng.licensehub.activation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ActivationMachineRequest(
    @NotNull UUID productId,
    @NotBlank @Size(max = 100) String licenseKey,
    @NotBlank @Size(max = 512) String machineFingerprint,
    @Size(max = 150) String machineName) {}
