package com.weng.licensehub.product.api;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {

}
