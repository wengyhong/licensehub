package com.weng.licensehub.activation.application;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.InvalidLicenseKeyException;
import com.weng.licensehub.license.application.LicenseKeyVerifier;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.shared.security.Sha256Hasher;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivationService {

  private final LicenseKeyVerifier licenseKeyVerifier;
  private final Sha256Hasher sha256Hasher;
  private final MachineActivationRepository machineActivationRepository;
  private final Clock clock;
  private final LicenseRepository licenseRepository;

  public ActivationService(
      LicenseKeyVerifier licenseKeyVerifier,
      Sha256Hasher sha256Hasher,
      MachineActivationRepository machineActivationRepository,
      Clock clock,
      LicenseRepository licenseRepository) {
    this.licenseKeyVerifier = licenseKeyVerifier;
    this.sha256Hasher = sha256Hasher;
    this.machineActivationRepository = machineActivationRepository;
    this.clock = clock;
    this.licenseRepository = licenseRepository;
  }

  @Transactional
  public MachineActivation activate(
      UUID productId, String fullLicenseKey, String machineFingerprint, String machineName) {
    License verifiedLicense = licenseKeyVerifier.verify(fullLicenseKey);

    License license =
        licenseRepository
            .findByIdForUpdate(verifiedLicense.getId())
            .orElseThrow(() -> new LicenseNotActivatableException());

    requireProduct(license, productId);

    Instant now = clock.instant();

    if (!license.canActivateAt(now)) {
      throw new LicenseNotActivatableException();
    }

    String fingerprintHash = sha256Hasher.hash(machineFingerprint);

    Optional<MachineActivation> existing =
        machineActivationRepository.findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
            license.getId(), fingerprintHash);

    if (existing.isPresent()) {
      MachineActivation activation = existing.orElseThrow();

      activation.markSeen(now);
      return activation;
    }

    long activeCount =
        machineActivationRepository.countByLicense_IdAndDeactivatedAtIsNull(license.getId());

    if (activeCount >= license.getMaxActivations()) {
      throw new ActivationLimitExceededException();
    }

    MachineActivation activation = new MachineActivation(license, fingerprintHash, machineName);

    return machineActivationRepository.save(activation);
  }

  @Transactional
  public void deactivate(UUID productId, String licenseKey, String machineFingerprint) {
    License verifiedLicense = licenseKeyVerifier.verify(licenseKey);

    License license =
        licenseRepository
            .findByIdForUpdate(verifiedLicense.getId())
            .orElseThrow(LicenseNotActivatableException::new);

    requireProduct(license, productId);

    String fingerprintHash = sha256Hasher.hash(machineFingerprint);

    machineActivationRepository
        .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
            license.getId(), fingerprintHash)
        .ifPresent(a -> a.deactivate(clock.instant()));
  }

  @Transactional
  public MachineActivation validate(UUID productId, String licenseKey, String machineFingerprint) {
    License verifiedLicense = licenseKeyVerifier.verify(licenseKey);
    Instant now = clock.instant();

    requireProduct(verifiedLicense, productId);

    if (!verifiedLicense.canActivateAt(now)) {
      throw new LicenseNotActivatableException();
    }

    String fingerprintHash = sha256Hasher.hash(machineFingerprint);

    MachineActivation machineActivation =
        machineActivationRepository
            .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                verifiedLicense.getId(), fingerprintHash)
            .orElseThrow(() -> new MachineNotActivatedException());

    machineActivation.markSeen(clock.instant());

    return machineActivation;
  }

  private void requireProduct(License license, UUID productId) {
    if (!productId.equals(license.getProduct().getId())) {
      throw new InvalidLicenseKeyException();
    }
  }
}
