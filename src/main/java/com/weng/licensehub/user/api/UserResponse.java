package com.weng.licensehub.user.api;

import java.time.Instant;
import java.util.UUID;

import com.weng.licensehub.user.domain.UserAccount;

public record UserResponse(

        UUID id,
        String email,
        Instant createdAt

) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getEmail(),
                account.getCreatedAt());
    }
}
