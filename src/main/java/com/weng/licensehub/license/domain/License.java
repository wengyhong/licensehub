package com.weng.licensehub.license.domain;

import java.time.Instant;
import java.util.UUID;

import com.weng.licensehub.product.domain.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "licenses")
public class License {

    public License(Product product, String keyId, String keyHash, String customerEmail, int maxActivations,
            Instant expiresAt) {

        if (maxActivations <= 0) {
            throw new IllegalArgumentException(
                    "maxActivations must be greater than zero");
        }
        this.product = product;
        this.keyId = keyId;
        this.keyHash = keyHash;
        this.customerEmail = customerEmail;
        this.maxActivations = maxActivations;
        this.expiresAt = expiresAt;

        this.status = LicenseStatus.ACTIVE;

    }

    protected License() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "key_id", length = 16, nullable = false, unique = true, updatable = false)
    private String keyId;

    @Column(name = "key_hash", length = 64, nullable = false, updatable = false)
    private String keyHash;

    @Column(name = "customer_email", length = 320, nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LicenseStatus status;

    @Column(name = "max_activations", nullable = false)
    private int maxActivations;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public LicenseStatus getStatus() {
        return status;
    }

    public int getMaxActivations() {
        return maxActivations;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
