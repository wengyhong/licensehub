package com.weng.licensehub.license.api;

import java.util.List;
import java.util.UUID;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.license.application.IssuedLicense;

import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService service) {
        this.licenseService = service;
    }

    @PostMapping("/products/{productId}/licenses")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedLicenseResponse issue(
            @PathVariable UUID productId,

            @Valid @RequestBody CreateLicenseRequest request) {
        IssuedLicense issued = licenseService.issue(productId, request.customerEmail(), request.maxActivations(),
                request.expiresAt());

        return new IssuedLicenseResponse(toResponse(issued.license()), issued.fullKey());
    }

    private LicenseResponse toResponse(License license) {
        return new LicenseResponse(license.getId(), license.getProduct().getId(), license.getKeyId(),
                license.getCustomerEmail(), license.getStatus(), license.getMaxActivations(), license.getExpiresAt(),
                license.getCreatedAt(), license.getUpdatedAt());
    }

    @GetMapping("/products/{productId}/licenses")
    public List<LicenseResponse> findAllByProductId(@PathVariable UUID productId)
    {
        return licenseService.findAllByProductId(productId).stream().map(this::toResponse).toList();
    }
    @GetMapping("/licenses/{licenseId}")
    public LicenseResponse findById(@PathVariable UUID licenseId)
    {
        return toResponse(licenseService.getById(licenseId));
    }
}
