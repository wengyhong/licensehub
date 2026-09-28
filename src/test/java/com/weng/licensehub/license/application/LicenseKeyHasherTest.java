package com.weng.licensehub.license.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.shared.security.Sha256Hasher;

class Sha256HasherTest {

    private final Sha256Hasher hasher =
            new Sha256Hasher();

    @Test
    void matchesSecretWithItsHash() {
        String secret = "correct-secret";
        String hash = hasher.hash(secret);

        boolean result = hasher.matches(secret, hash);

        assertThat(result).isTrue();
    }

    @Test
    void doesNotMatchDifferentSecret() {
        String hash = hasher.hash("correct-secret");

        boolean result =
                hasher.matches("wrong-secret", hash);

        assertThat(result).isFalse();
    }
}