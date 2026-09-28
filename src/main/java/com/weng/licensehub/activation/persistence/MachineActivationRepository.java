package com.weng.licensehub.activation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weng.licensehub.activation.domain.MachineActivation;

public interface MachineActivationRepository extends JpaRepository<MachineActivation, UUID> {


    //Is this exact machine already actively registered to this license?
    Optional<MachineActivation> findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(UUID licenseId, String machineFingerprintHash);

    //How many active machines currently use this license?
    long countByLicense_IdAndDeactivatedAtIsNull(UUID licenseId);
}
