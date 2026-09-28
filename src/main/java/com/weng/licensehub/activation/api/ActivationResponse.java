package com.weng.licensehub.activation.api;

import java.time.Instant;
import java.util.UUID;

public record ActivationResponse(

    UUID id,
    UUID licenseId,
    String machineName,
    Instant activatedAt,
    Instant lastSeenAt

) {

}
