package com.weng.licensehub.user.api;

import com.weng.licensehub.user.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, Instant createdAt) {

  public static UserResponse from(UserAccount account) {
    return new UserResponse(account.getId(), account.getEmail(), account.getCreatedAt());
  }
}
