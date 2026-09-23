package com.weng.licensehub.product.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name="products")
public class Product {

    @Id
    @GeneratedValue (strategy = GenerationType.UUID)

    private UUID id;

    @Column (nullable = false, length = 150)
    private String name;

    @Column (length = 1000)
    private String description;

    @Column(name="created_at", nullable = false, updatable =  false)
    private Instant createdAt;


    @Column (name="updated_at", nullable = false)
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

    protected Product(){}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Product(String name, String description) {
    this.name = name;
    this.description = description;
}
}
