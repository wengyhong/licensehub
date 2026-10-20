package com.weng.licensehub.activation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.weng.licensehub.shared.security.Sha256Hasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivationTokenGeneratorTest {

  private ActivationTokenGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new ActivationTokenGenerator(new Sha256Hasher());
  }

  @Test
  void generatesUniqueTokens() {
    GeneratedActivationToken first = generator.generate();
    GeneratedActivationToken second = generator.generate();

    assertThat(first.fullToken()).isNotEqualTo(second.fullToken());
    assertThat(first.tokenHash()).isNotEqualTo(second.tokenHash());
  }

  @Test
  void generatesExpectedTokenAndHashFormats() {
    GeneratedActivationToken generated = generator.generate();

    assertThat(generated.fullToken()).matches("LA_[A-Za-z0-9_-]{43}");
    assertThat(generated.tokenHash()).matches("[0-9a-f]{64}");
  }

  @Test
  void hashesTheFullToken() throws NoSuchAlgorithmException {
    GeneratedActivationToken generated = generator.generate();
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    String expectedHash =
        HexFormat.of()
            .formatHex(digest.digest(generated.fullToken().getBytes(StandardCharsets.UTF_8)));

    assertThat(generated.tokenHash()).isEqualTo(expectedHash);
  }
}
