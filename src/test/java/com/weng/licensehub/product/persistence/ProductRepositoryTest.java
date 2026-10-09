package com.weng.licensehub.product.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

        @Container
        @ServiceConnection
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private UserAccountRepository userAccountRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void queriesProductsByOwner() {

                UserAccount alice = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "alice@example.com",
                                                "{noop}unused"));

                UserAccount bob = userAccountRepository.saveAndFlush(
                                new UserAccount(
                                                "bob@example.com",
                                                "{noop}unused"));

                Product aliceProduct = productRepository.saveAndFlush(
                                new Product(
                                                alice,
                                                "Alice Product",
                                                null));

                Product bobProduct = productRepository.saveAndFlush(
                                new Product(
                                                bob,
                                                "Bob Product",
                                                null));

                UUID aliceId = alice.getId();
                UUID aliceProductId = aliceProduct.getId();
                UUID bobProductId = bobProduct.getId();

                entityManager.clear();

                Pageable pageable = PageRequest.of(0, 10);

                Page<Product> aliceProducts = productRepository
                                .findAllByOwner_Id(
                                                aliceId,
                                                pageable);

                assertThat(aliceProducts.getContent())
                                .extracting(Product::getId)
                                .containsExactly(aliceProductId);

                assertThat(aliceProducts.getTotalElements())
                                .isEqualTo(1);

                assertThat(productRepository.findByIdAndOwner_Id(
                                aliceProductId,
                                aliceId))
                                .isPresent();

                assertThat(productRepository.findByIdAndOwner_Id(
                                bobProductId,
                                aliceId))
                                .isEmpty();
        }
}