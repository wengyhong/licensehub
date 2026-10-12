package com.weng.licensehub.activation.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActivationRateLimiterTest {

    @Test
    void rejectsRequestsAfterLimitIsExhausted() {
        ActivationRateLimiter limiter =
                new ActivationRateLimiter(2);

        assertThat(limiter.tryConsume("192.0.2.10"))
                .isTrue();

        assertThat(limiter.tryConsume("192.0.2.10"))
                .isTrue();

        assertThat(limiter.tryConsume("192.0.2.10"))
                .isFalse();
    }

    @Test
    void tracksClientsIndependently() {
        ActivationRateLimiter limiter =
                new ActivationRateLimiter(1);

        assertThat(limiter.tryConsume("192.0.2.10"))
                .isTrue();

        assertThat(limiter.tryConsume("192.0.2.10"))
                .isFalse();

        assertThat(limiter.tryConsume("192.0.2.20"))
                .isTrue();
    }
}