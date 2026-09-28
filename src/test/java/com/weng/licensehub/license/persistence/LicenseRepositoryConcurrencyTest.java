package com.weng.licensehub.license.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
class LicenseRepositoryConcurrencyTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void secondTransactionWaitsForLicenseLock()
            throws Exception {

        Product product = productRepository.save(
                new Product("Product", null));

        License license = licenseRepository.saveAndFlush(
                new License(
                        product,
                        "ABCDEF1234567890",
                        "a".repeat(64),
                        "customer@example.com",
                        2,
                        null));

        UUID licenseId = license.getId();

        TransactionTemplate transactions =
                new TransactionTemplate(transactionManager);

        CountDownLatch firstHasLock =
                new CountDownLatch(1);

        CountDownLatch releaseFirst =
                new CountDownLatch(1);

        CountDownLatch secondStarted =
                new CountDownLatch(1);

        CountDownLatch secondHasLock =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> firstTransaction =
                    executor.submit(() ->
                            transactions.executeWithoutResult(
                                    status -> {
                                        licenseRepository
                                                .findByIdForUpdate(
                                                        licenseId)
                                                .orElseThrow();

                                        firstHasLock.countDown();

                                        await(releaseFirst);
                                    }));

            assertThat(firstHasLock.await(
                    5,
                    TimeUnit.SECONDS))
                    .isTrue();

            Future<?> secondTransaction =
                    executor.submit(() ->
                            transactions.executeWithoutResult(
                                    status -> {
                                        secondStarted.countDown();

                                        licenseRepository
                                                .findByIdForUpdate(
                                                        licenseId)
                                                .orElseThrow();

                                        secondHasLock.countDown();
                                    }));

            assertThat(secondStarted.await(
                    5,
                    TimeUnit.SECONDS))
                    .isTrue();

            assertThat(secondHasLock.await(
                    300,
                    TimeUnit.MILLISECONDS))
                    .as("second transaction should be blocked")
                    .isFalse();

            releaseFirst.countDown();

            firstTransaction.get(
                    5,
                    TimeUnit.SECONDS);

            assertThat(secondHasLock.await(
                    5,
                    TimeUnit.SECONDS))
                    .isTrue();

            secondTransaction.get(
                    5,
                    TimeUnit.SECONDS);

        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            boolean completed =
                    latch.await(5, TimeUnit.SECONDS);

            if (!completed) {
                throw new IllegalStateException(
                        "Timed out waiting for test signal");
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Test thread was interrupted",
                    exception);
        }
    }
}