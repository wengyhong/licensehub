package com.weng.licensehub.user.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.user.application.UserRegistrationService;
import com.weng.licensehub.user.domain.UserAccount;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.context.SecurityContext;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(
            UserRegistrationService userRegistrationService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {

        this.userRegistrationService = userRegistrationService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    private final UserRegistrationService userRegistrationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)

    public UserResponse register(

            @Valid @RequestBody RegisterUserRequest request) {
        UserAccount account = userRegistrationService.register(request.email(), request.password());

        return UserResponse.from(account);
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest loginRequest,

            HttpServletRequest httpRequest,
            HttpServletResponse httpReponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.email(), loginRequest.password()));

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpReponse);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(securityContext, httpRequest, httpReponse);

        return new LoginResponse(authentication.getName());

    }

    @GetMapping ("/me")
    public CurrentUserResponse currentUser(Authentication authentication)
    {
        return new CurrentUserResponse(authentication.getName());
    }
}
