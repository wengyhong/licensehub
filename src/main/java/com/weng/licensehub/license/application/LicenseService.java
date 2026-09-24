package com.weng.licensehub.license.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.application.ProductNotFoundException;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.product.persistence.ProductRepository;
@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductRepository productRepository;
    private final LicenseKeyGenerator licenseKeyGenerator;

    public LicenseService(
        LicenseRepository licenseRepo,
        ProductRepository productRepo,
        LicenseKeyGenerator generator) {

        this.licenseRepository = licenseRepo;
        this.productRepository = productRepo;
        this.licenseKeyGenerator = generator;

        }

    @Transactional
    public IssuedLicense issue(

        UUID productId,
        String customerEmail,
        int maxActivations,
        Instant expiresAt
    ){

        Product product = productRepository.findById(productId).orElseThrow(()-> new ProductNotFoundException(productId));

        GeneratedLicenseKey generatedKey = licenseKeyGenerator.generate();

        License license = new License(product, generatedKey.keyId(), generatedKey.keyHash(), customerEmail.trim(), maxActivations, expiresAt);

        License savedLicense = licenseRepository.save(license);

        return new IssuedLicense(savedLicense, generatedKey.fullKey());

    }

    @Transactional (readOnly = true)
    public License getById(UUID licenseId)
    {
        return licenseRepository.findById(licenseId).orElseThrow(()-> new LicenseNotFoundException(licenseId));
    }
    @Transactional (readOnly = true)
    public List<License> findAllByProductId(UUID productId)
    {
        if(!productRepository.existsById(productId))
        {
            throw new ProductNotFoundException(productId);
        }

        return licenseRepository.findAllByProduct_IdOrderByCreatedAtDesc(productId);
    }

}
