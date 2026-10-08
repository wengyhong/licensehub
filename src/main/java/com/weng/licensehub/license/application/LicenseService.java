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
import com.weng.licensehub.user.domain.UserAccount;
import com.weng.licensehub.user.persistence.UserAccountRepository;

@Service
public class LicenseService {

        private final LicenseRepository licenseRepository;
        private final ProductRepository productRepository;
        private final LicenseKeyGenerator licenseKeyGenerator;
        private final UserAccountRepository userAccountRepository;

        public LicenseService(
                        LicenseRepository licenseRepo,
                        ProductRepository productRepo,
                        LicenseKeyGenerator generator,
                        UserAccountRepository userAccountRepository) {

                this.licenseRepository = licenseRepo;
                this.productRepository = productRepo;
                this.licenseKeyGenerator = generator;
                this.userAccountRepository = userAccountRepository;

        }

        private UserAccount requireOwner(String ownerEmail) {
                return userAccountRepository
                                .findByEmailIgnoreCase(ownerEmail)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Authenticated user account was not found"));
        }

        private Product requireOwnedProduct(
                        UUID productId,
                        String ownerEmail) {

                UserAccount owner = requireOwner(ownerEmail);

                return productRepository
                                .findByIdAndOwner_Id(
                                                productId,
                                                owner.getId())
                                .orElseThrow(() -> new ProductNotFoundException(productId));
        }

        private IssuedLicense issueForProduct(
                        Product product,
                        String customerEmail,
                        int maxActivations,
                        Instant expiresAt) {

                GeneratedLicenseKey generatedKey = licenseKeyGenerator.generate();

                License license = new License(
                                product,
                                generatedKey.keyId(),
                                generatedKey.keyHash(),
                                customerEmail.trim(),
                                maxActivations,
                                expiresAt);

                License savedLicense = licenseRepository.save(license);

                return new IssuedLicense(
                                savedLicense,
                                generatedKey.fullKey());
        }

        @Transactional
        public IssuedLicense issueForOwner(
                        UUID productId,
                        String ownerEmail,
                        String customerEmail,
                        int maxActivations,
                        Instant expiresAt) {

                Product product = requireOwnedProduct(
                                productId,
                                ownerEmail);

                return issueForProduct(
                                product,
                                customerEmail,
                                maxActivations,
                                expiresAt);
        }

        @Transactional(readOnly = true)
        public List<License> findAllByProductIdForOwner(
                        UUID productId,
                        String ownerEmail) {

                requireOwnedProduct(productId, ownerEmail);

                return licenseRepository
                                .findAllByProduct_IdOrderByCreatedAtDesc(
                                                productId);
        }

        @Transactional(readOnly = true)
        public License getByIdForOwner(UUID productId, String email) {
                UserAccount owner = requireOwner(email);

                return licenseRepository.findByIdAndProduct_Owner_Id(productId, owner.getId())
                                .orElseThrow(() -> new LicenseNotFoundException(productId));
        }

        @Transactional

        public void revokeForOwner(UUID licenseId, String ownerEmail) {
                UserAccount owner = requireOwner(ownerEmail);

                License license = licenseRepository.findByIdAndProduct_Owner_Id(licenseId, owner.getId())
                                .orElseThrow(() -> new LicenseNotFoundException(licenseId));

                license.revoke();

        }

}
