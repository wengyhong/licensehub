package com.weng.licensehub.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.weng.licensehub.shared.security.UserAccountDetailsService;
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private UserAccountDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserAccountDetailsService(
                userAccountRepository);
    }

    @Test
    void loadsUserDetailsFromUserAccount() {
        UserAccount account = new UserAccount(
                "Alice@example.com",
                "{bcrypt}encoded-password");

        when(userAccountRepository.findByEmailIgnoreCase(
                "alice@example.com"))
                .thenReturn(Optional.of(account));

        UserDetails result =
                userDetailsService.loadUserByUsername(
                        "alice@example.com");

        assertThat(result.getUsername())
                .isEqualTo("Alice@example.com");

        assertThat(result.getPassword())
                .isEqualTo("{bcrypt}encoded-password");

        assertThat(result.getAuthorities())
                .extracting(authority ->
                        authority.getAuthority())
                .containsExactly("ROLE_USER");

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();

        verify(userAccountRepository)
                .findByEmailIgnoreCase("alice@example.com");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        when(userAccountRepository.findByEmailIgnoreCase(
                "missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername(
                        "missing@example.com"))
                .isInstanceOf(
                        UsernameNotFoundException.class)
                .hasMessage("User was not found")
                .hasMessageNotContaining(
                        "missing@example.com");
    }
}