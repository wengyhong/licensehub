package com.weng.licensehub.license.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.security.Principal;

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

            @Valid @RequestBody CreateLicenseRequest request,
            Principal principal) {
        IssuedLicense issued = licenseService.issueForOwner(productId, principal.getName(), request.customerEmail(),
                request.maxActivations(),
                request.expiresAt());

        return new IssuedLicenseResponse(toResponse(issued.license()), issued.fullKey());
    }

    private LicenseResponse toResponse(License license) {
        return new LicenseResponse(license.getId(), license.getProduct().getId(), license.getKeyId(),
                license.getCustomerEmail(), license.getStatus(), license.getMaxActivations(), license.getExpiresAt(),
                license.getCreatedAt(), license.getUpdatedAt());
    }

    @GetMapping("/products/{productId}/licenses")
    public Page<LicenseResponse> findAllByProductId(@PathVariable UUID productId, Principal principal, @PageableDefault (size = 20, sort ="createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return licenseService.findAllByProductIdForOwner(productId, principal.getName(), pageable).map(this::toResponse);
    }

    @GetMapping("/licenses/{licenseId}")
    public LicenseResponse findById(@PathVariable UUID licenseId, Principal principal) {
        return toResponse(licenseService.getByIdForOwner(licenseId, principal.getName()));
    }

    @PostMapping("/licenses/{licenseId}/revoke")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID licenseId,
            Principal principal) {
        licenseService.revokeForOwner(licenseId, principal.getName());

        return ResponseEntity.noContent().build();

    }
}
