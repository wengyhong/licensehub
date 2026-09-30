package com.weng.licensehub.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new UserRegistrationService(
                userAccountRepository,
                passwordEncoder);
    }

    @Test
    void hashesPasswordBeforeSavingUser() {
        String rawPassword = "correct-horse-battery-staple";
        String passwordHash = "{bcrypt}encoded-password";

        when(userAccountRepository.findByEmailIgnoreCase(
                "Alice@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(passwordHash);

        when(userAccountRepository.saveAndFlush(
                any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount result = registrationService.register(
                "  Alice@example.com  ",
                rawPassword);

        ArgumentCaptor<UserAccount> accountCaptor =
                ArgumentCaptor.forClass(UserAccount.class);

        verify(userAccountRepository)
                .saveAndFlush(accountCaptor.capture());

        UserAccount saved = accountCaptor.getValue();

        assertThat(saved.getEmail())
                .isEqualTo("Alice@example.com");

        assertThat(saved.getPasswordHash())
                .isEqualTo(passwordHash);

        assertThat(saved.getPasswordHash())
                .isNotEqualTo(rawPassword);

        assertThat(result).isSameAs(saved);

        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void rejectsEmailThatIsAlreadyRegistered() {
        UserAccount existingAccount =
                new UserAccount(
                        "alice@example.com",
                        "{bcrypt}existing-hash");

        when(userAccountRepository.findByEmailIgnoreCase(
                "alice@example.com"))
                .thenReturn(Optional.of(existingAccount));

        assertThatThrownBy(() ->
                registrationService.register(
                        "alice@example.com",
                        "correct-horse-battery-staple"))
                .isInstanceOf(
                        EmailAlreadyRegisteredException.class);

        verifyNoInteractions(passwordEncoder);

        verify(userAccountRepository, never())
                .saveAndFlush(any(UserAccount.class));
    }

    @Test
    void translatesConcurrentDuplicateEmailViolation() {
        String rawPassword = "correct-horse-battery-staple";

        when(userAccountRepository.findByEmailIgnoreCase(
                "alice@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(rawPassword))
                .thenReturn("{bcrypt}encoded-password");

        when(userAccountRepository.saveAndFlush(
                any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate email"));

        assertThatThrownBy(() ->
                registrationService.register(
                        "alice@example.com",
                        rawPassword))
                .isInstanceOf(
                        EmailAlreadyRegisteredException.class)
                .hasCauseInstanceOf(
                        DataIntegrityViolationException.class);
    }
}