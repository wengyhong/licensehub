package com.weng.licensehub.user.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.user.application.UserRegistrationService;
import com.weng.licensehub.user.domain.UserAccount;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@ResponseStatus (HttpStatus.CREATED)
public class AuthController {


    public AuthController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    private final UserRegistrationService userRegistrationService;
    @PostMapping("/register")
    public UserResponse register(

        @Valid
        @RequestBody RegisterUserRequest request
    )
    {
        UserAccount account = userRegistrationService.register(request.email(), request.password());

        return UserResponse.from(account);
    }


}
