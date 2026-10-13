package com.weng.licensehub.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.user.domain.UserAccount;

public class ProductTest {

  @Test
void updatesProductDetails() {
    UserAccount owner =
            new UserAccount(
                    "owner@example.com",
                    "test-password-hash");

    Product product =
            new Product(owner, "Original", "Original description");

    product.updateDetails(
            " Updated Product ",
            " Updated description ");

    assertThat(product.getName())
            .isEqualTo("Updated Product");

    assertThat(product.getDescription())
            .isEqualTo("Updated description");
}

  @Test
void rejectsBlankNameWhenUpdating() {
    UserAccount owner =
            new UserAccount(
                    "owner@example.com",
                    "test-password-hash");

    Product product =
            new Product(owner, "Original", null);

    assertThatThrownBy(
            () -> product.updateDetails(" ", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("name must not be blank");
}
}
