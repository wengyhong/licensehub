package com.weng.licensehub.shared.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.product.application.ProductHasLicensesException;
import com.weng.licensehub.product.application.ProductNotFoundException;
import com.weng.licensehub.user.application.EmailAlreadyRegisteredException;
import com.weng.licensehub.activation.application.ActivationLimitExceededException;
import com.weng.licensehub.activation.application.LicenseNotActivatableException;
import com.weng.licensehub.license.application.InvalidLicenseKeyException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());

        problem.setTitle("Product not found");

        return problem;

    }

    @ExceptionHandler(LicenseNotFoundException.class)
    public ProblemDetail handleLicenseNotFound(LicenseNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());

        problem.setTitle("License not found");

        return problem;

    }

    @ExceptionHandler(InvalidLicenseKeyException.class)
    public ProblemDetail handleInvalidLicenseKey(
            InvalidLicenseKeyException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());

        problem.setTitle("Invalid license key");

        return problem;
    }

    @ExceptionHandler(LicenseNotActivatableException.class)
    public ProblemDetail handleLicenseNotActivatable(
            LicenseNotActivatableException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                exception.getMessage());

        problem.setTitle("License not activatable");

        return problem;
    }

    @ExceptionHandler(ActivationLimitExceededException.class)
    public ProblemDetail handleActivationLimitExceeded(
            ActivationLimitExceededException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());

        problem.setTitle("Activation limit exceeded");

        return problem;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());

        problem.setTitle("Email already registered");

        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");

        problem.setTitle("Authentication failed");

        return problem;
    }

    @ExceptionHandler (ProductHasLicensesException.class)
    public ProblemDetail handleProductHasLicenses(ProductHasLicensesException exception)
    {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());

        problem.setTitle("Product has existing licenses");

        return problem;
    }

}
