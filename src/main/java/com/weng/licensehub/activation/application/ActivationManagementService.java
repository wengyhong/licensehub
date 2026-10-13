package com.weng.licensehub.activation.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.LicenseService;

@Service
public class ActivationManagementService {

    private final Clock clock;
    private final LicenseService licenseService;
    private final MachineActivationRepository activationRepository;

    public ActivationManagementService(LicenseService licenseService,
            MachineActivationRepository activationRepository, Clock clock) {
        this.licenseService = licenseService;
        this.activationRepository = activationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<MachineActivation> findAllForOwner(
            UUID licenseId,
            String ownerEmail,
            Pageable pageable) {

        licenseService.getByIdForOwner(licenseId, ownerEmail);

        return activationRepository.findAllByLicense_Id(licenseId, pageable);
    }

    @Transactional
    public void deactivateForOwner(UUID licenseId, UUID activationId, String ownerEmail) {

        licenseService.getByIdForOwner(licenseId, ownerEmail);
        MachineActivation activation = activationRepository.findByIdAndLicense_Id(activationId, licenseId)
                .orElseThrow(() -> new MachineActivationNotFoundException(activationId));

        activation.deactivate(clock.instant());
    }

}
