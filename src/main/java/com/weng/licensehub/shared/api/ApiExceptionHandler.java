package com.weng.licensehub.shared.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.product.application.ProductNotFoundException;

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

}
