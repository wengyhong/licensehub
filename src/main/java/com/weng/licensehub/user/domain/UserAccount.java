package com.weng.licensehub.user.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private  String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;
    @Column(name = "created_at", nullable = false, updatable = false)

    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)

    private Instant updatedAt;

    public UserAccount(String email, String passwordHash)
    {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }

        this.email = email.strip();
        this.passwordHash = passwordHash;
    }
    protected UserAccount() {


    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        Instant now = Instant.now();
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
