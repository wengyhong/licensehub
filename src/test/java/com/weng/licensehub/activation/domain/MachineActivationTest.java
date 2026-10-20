package com.weng.licensehub.activation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.user.domain.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MachineActivationTest {

  @Test
  void markSeenAdvancesLastSeenTime() {
    MachineActivation activation = createActivation();

    activation.prePersist();

    Instant later = activation.getLastSeenAt().plusSeconds(60);

    activation.markSeen(later);

    assertThat(activation.isActive()).isTrue();
    assertThat(activation.getLastSeenAt()).isEqualTo(later);
  }

  @Test
  void deactivatedActivationIsNoLongerActive() {
    MachineActivation activation = createActivation();

    activation.prePersist();

    Instant deactivatedAt = activation.getLastSeenAt().plusSeconds(60);

    activation.deactivate(deactivatedAt);

    assertThat(activation.isActive()).isFalse();
    assertThat(activation.getDeactivatedAt()).isEqualTo(deactivatedAt);
  }

  @Test
  void cannotMarkDeactivatedActivationAsSeen() {
    MachineActivation activation = createActivation();

    activation.prePersist();
    activation.deactivate(Instant.now());

    assertThatThrownBy(() -> activation.markSeen(Instant.now()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rotatesActivationTokenHash() {
    MachineActivation activation = createActivation();

    activation.rotateActivationToken("c".repeat(64));
    activation.rotateActivationToken("d".repeat(64));

    assertThat(activation.getActivationTokenHash()).isEqualTo("d".repeat(64));
  }

  @Test
  void rejectsInvalidActivationTokenHash() {
    MachineActivation activation = createActivation();

    assertThatThrownBy(() -> activation.rotateActivationToken("too-short"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("activationTokenHash must be a 64-character SHA-256 hash");
  }

  @Test
  void cannotRotateTokenForDeactivatedActivation() {
    MachineActivation activation = createActivation();
    activation.prePersist();
    activation.deactivate(activation.getLastSeenAt());

    assertThatThrownBy(() -> activation.rotateActivationToken("c".repeat(64)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("A deactivated machine activation cannot receive a new token");
  }

  private MachineActivation createActivation() {
    UserAccount owner = new UserAccount("owner@example.com", "test-password-hash");

    Product product = new Product(owner, "Test Product", null);

    License license =
        new License(product, "0123456789ABCDEF", "a".repeat(64), "customer@example.com", 1, null);

    return new MachineActivation(license, "b".repeat(64), "Development laptop");
  }
}
