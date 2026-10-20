package com.weng.licensehub.activation.application;

import com.weng.licensehub.shared.security.Sha256Hasher;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ActivationTokenGenerator {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Sha256Hasher sha256Hasher;

  public ActivationTokenGenerator(Sha256Hasher sha256Hasher) {
    this.sha256Hasher = sha256Hasher;
  }

  public GeneratedActivationToken generate() {
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(tokenBytes);

    String encodedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    String fullToken = "LA_" + encodedToken;
    String tokenHash = sha256Hasher.hash(fullToken);

    return new GeneratedActivationToken(tokenHash, fullToken);
  }
}
