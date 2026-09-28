package com.weng.licensehub.activation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.product.domain.Product;

class MachineActivationTest {

    @Test
    void markSeenAdvancesLastSeenTime() {
        Product product = new Product("Product", null);

        License license = new License(
                product,
                "0123456789ABCDEF",
                "a".repeat(64),
                "customer@example.com",
                1,
                null);

        MachineActivation activation = new MachineActivation(
                license,
                "b".repeat(64),
                "Development laptop");

        activation.prePersist();

        Instant later = activation.getLastSeenAt().plusSeconds(60);

        activation.markSeen(later);

        assertThat(activation.isActive()).isTrue();
        assertThat(activation.getLastSeenAt())
                .isEqualTo(later);
    }

    @Test
    void deactivatedActivationIsNoLongerActive() {
        Product product = new Product("Product", null);

        License license = new License(
                product,
                "0123456789ABCDEF",
                "a".repeat(64),
                "customer@example.com",
                1,
                null);

        MachineActivation activation = new MachineActivation(
                license,
                "b".repeat(64),
                "Development laptop");

        activation.prePersist();

        Instant deactivatedAt = activation.getLastSeenAt().plusSeconds(60);

        activation.deactivate(deactivatedAt);

        assertThat(activation.isActive()).isFalse();
        assertThat(activation.getDeactivatedAt())
                .isEqualTo(deactivatedAt);
    }

    @Test
    void cannotMarkDeactivatedActivationAsSeen() {
        Product product = new Product("Product", null);

        License license = new License(
                product,
                "0123456789ABCDEF",
                "a".repeat(64),
                "customer@example.com",
                1,
                null);

        MachineActivation activation = new MachineActivation(
                license,
                "b".repeat(64),
                "Development laptop");

        activation.prePersist();
        activation.deactivate(Instant.now());

        assertThatThrownBy(
                () -> activation.markSeen(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}