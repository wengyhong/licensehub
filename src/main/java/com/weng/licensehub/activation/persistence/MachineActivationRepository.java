package com.weng.licensehub.activation.persistence;

import com.weng.licensehub.activation.domain.MachineActivation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineActivationRepository extends JpaRepository<MachineActivation, UUID> {

  // Is this exact machine already actively registered to this license?
  Optional<MachineActivation> findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
      UUID licenseId, String machineFingerprintHash);

  // How many active machines currently use this license?
  long countByLicense_IdAndDeactivatedAtIsNull(UUID licenseId);

  Page<MachineActivation> findAllByLicense_Id(UUID licenseId, Pageable pageable);

  Optional<MachineActivation> findByIdAndLicense_Id(UUID activationId, UUID licenseId);
}
