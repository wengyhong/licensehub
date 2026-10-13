package com.weng.licensehub.activation.api;

import java.time.Instant;
import java.util.UUID;

public record MachineActivationResponse(

    UUID id,
    UUID licenseId,
    String machineName,
    Instant activatedAt,
    Instant lastSeenAt,
    Instant deactivatedAt,
    boolean active
) {

}
