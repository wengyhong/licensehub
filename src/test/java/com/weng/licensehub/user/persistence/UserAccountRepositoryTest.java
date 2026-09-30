package com.weng.licensehub.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.weng.licensehub.user.domain.UserAccount;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
class UserAccountRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsUserByEmailIgnoringCase() {
        UserAccount saved = userAccountRepository.saveAndFlush(
                new UserAccount(
                        "Alice@Example.com",
                        "example-password-hash"));

        entityManager.clear();

        Optional<UserAccount> result =
                userAccountRepository.findByEmailIgnoreCase(
                        "alice@example.com");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId())
                .isEqualTo(saved.getId());
        assertThat(result.orElseThrow().getEmail())
                .isEqualTo("Alice@Example.com");
    }

    @Test
    void rejectsEmailThatDiffersOnlyByCase() {
        userAccountRepository.saveAndFlush(
                new UserAccount(
                        "Alice@Example.com",
                        "first-password-hash"));

        assertThatThrownBy(() ->
                userAccountRepository.saveAndFlush(
                        new UserAccount(
                                "ALICE@example.com",
                                "second-password-hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}