package com.weng.licensehub.shared.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@Service
public class UserAccountDetailsService implements UserDetailsService{

    private final UserAccountRepository userAccountRepository;

    public UserAccountDetailsService(UserAccountRepository userAccountRepository)
    {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        UserAccount account = userAccountRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User was not found"));

        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles("USER")
                .build();
    }

}
