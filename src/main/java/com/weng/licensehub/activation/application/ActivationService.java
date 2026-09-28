package com.weng.licensehub.activation.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.LicenseKeyVerifier;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.shared.security.Sha256Hasher;
import com.weng.licensehub.license.persistence.LicenseRepository;
@Service

public class ActivationService {

    private final LicenseKeyVerifier licenseKeyVerifier;
    private final Sha256Hasher sha256Hasher;
    private final MachineActivationRepository machineActivationRepository;
    private final Clock clock;
    private final LicenseRepository licenseRepository;


    public ActivationService(LicenseKeyVerifier licenseKeyVerifier, Sha256Hasher sha256Hasher,
            MachineActivationRepository machineActivationRepository, Clock clock, LicenseRepository licenseRepository) {
        this.licenseKeyVerifier = licenseKeyVerifier;
        this.sha256Hasher = sha256Hasher;
        this.machineActivationRepository = machineActivationRepository;
        this.clock = clock;
        this.licenseRepository = licenseRepository;
    }

    @Transactional
    public MachineActivation activate(
            String fullLicenseKey,
            String machineFingerprint,
            String machineName) {
        License verifiedLicense = licenseKeyVerifier.verify(fullLicenseKey);

        License license = licenseRepository.findByIdForUpdate(verifiedLicense.getId()).orElseThrow(()-> new LicenseNotActivatableException());

        Instant now = clock.instant();

        if (!license.canActivateAt(now)) {
            throw new LicenseNotActivatableException();
        }

        String fingerprintHash = sha256Hasher.hash(machineFingerprint);

        Optional<MachineActivation> existing = machineActivationRepository
                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(license.getId(), fingerprintHash);

        if (existing.isPresent()) {
            MachineActivation activation = existing.orElseThrow();

            activation.markSeen(now);
            return activation;
        }

        long activeCount = machineActivationRepository.countByLicense_IdAndDeactivatedAtIsNull(license.getId());

        if (activeCount >= license.getMaxActivations()) {
            throw new ActivationLimitExceededException();
        }

        MachineActivation activation = new MachineActivation(license, fingerprintHash, machineName);

        return machineActivationRepository.save(activation);

    }

    @Transactional
    public void deactivate(String licenseKey, String machineFingerprint)
    {
        License verifiedLicense = licenseKeyVerifier.verify(licenseKey);

        License license = licenseRepository.findByIdForUpdate(verifiedLicense.getId()).orElseThrow(LicenseNotActivatableException::new);

        String fingerprintHash = sha256Hasher.hash(machineFingerprint);

        machineActivationRepository.findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(license.getId(), fingerprintHash).ifPresent(a -> a.deactivate(clock.instant()));
    }

}
