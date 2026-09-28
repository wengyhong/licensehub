package com.weng.licensehub.activation.domain;

import java.time.Instant;
import java.util.UUID;

import com.weng.licensehub.license.domain.License;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "machine_activations")
public class MachineActivation {

    protected MachineActivation() {

    }

    public MachineActivation(License license, String machineFingerprintHash, String machineName) {
        this.license = license;
        this.machineFingerprintHash = machineFingerprintHash;
        this.machineName = machineName;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "license_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private License license;

    @Column(name = "machine_fingerprint_hash", nullable = false, length = 64, updatable = false)
    private String machineFingerprintHash;

    @Column(name = "machine_name", length = 150)
    private String machineName;

    @Column(name = "activated_at", nullable = false, updatable = false)
    private Instant activatedAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        activatedAt = now;
        lastSeenAt = now;
    }

    public UUID getId() {
        return id;
    }

    public License getLicense() {
        return license;
    }

    public String getMachineFingerprintHash() {
        return machineFingerprintHash;
    }

    public String getMachineName() {
        return machineName;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public boolean isActive()
    {
        return deactivatedAt == null;
    }

    public void markSeen(Instant seenAt)
    {
        Objects.requireNonNull(seenAt, "seenAt Must Not Be Null");

        if(!isActive())
        {
            throw new IllegalStateException("A deactivated machine activation cannot be updated");
        }

        if(lastSeenAt!= null && seenAt.isBefore((lastSeenAt)))
        {
            throw new IllegalStateException("seenAt must not be before lastSeenAt");
        }

        lastSeenAt = seenAt;
    }

    public void deactivate(Instant deactivatedAt)
    {
        Objects.requireNonNull(deactivatedAt, "deactivatedt must not be null");

        if(!isActive())
        {
            return;
        }

        this.deactivatedAt = deactivatedAt;
    }
}
