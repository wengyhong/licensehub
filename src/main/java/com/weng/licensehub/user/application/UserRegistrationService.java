package com.weng.licensehub.user.application;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@Service
public class UserRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String email, String rawPassword) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }

        String normalizedEmail = email.strip();

        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        UserAccount account = new UserAccount(normalizedEmail, passwordHash);
        try {
            return userAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException(exception);
        }

    }

}
