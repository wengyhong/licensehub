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
  private final ActivationTokenGenerator activationTokenGenerator;

  public ActivationService(
      LicenseKeyVerifier licenseKeyVerifier,
      Sha256Hasher sha256Hasher,
      MachineActivationRepository machineActivationRepository,
      Clock clock,
      LicenseRepository licenseRepository,
      ActivationTokenGenerator activationTokenGenerator) {
    this.licenseKeyVerifier = licenseKeyVerifier;
    this.sha256Hasher = sha256Hasher;
    this.machineActivationRepository = machineActivationRepository;
    this.clock = clock;
    this.licenseRepository = licenseRepository;
    this.activationTokenGenerator = activationTokenGenerator;
  }

  @Transactional
  public IssuedActivation activate(
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
      return issueActivationToken(activation);
    }

    long activeCount =
        machineActivationRepository.countByLicense_IdAndDeactivatedAtIsNull(license.getId());

    if (activeCount >= license.getMaxActivations()) {
      throw new ActivationLimitExceededException();
    }

    MachineActivation activation = new MachineActivation(license, fingerprintHash, machineName);
    IssuedActivation issued = issueActivationToken(activation);

    machineActivationRepository.save(activation);
    return issued;
  }

  @Transactional
  public void deactivate(UUID productId, String activationToken, String machineFingerprint) {
    MachineActivation activation =
        requireActiveActivation(productId, activationToken, machineFingerprint);

    activation.deactivate(clock.instant());
  }

  @Transactional
  public MachineActivation validate(
      UUID productId, String activationToken, String machineFingerprint) {
    MachineActivation activation =
        requireActiveActivation(productId, activationToken, machineFingerprint);
    License license = activation.getLicense();
    Instant now = clock.instant();

    if (!license.canActivateAt(now)) {
      throw new LicenseNotActivatableException();
    }

    activation.markSeen(now);

    return activation;
  }

  private void requireProduct(License license, UUID productId) {
    if (!productId.equals(license.getProduct().getId())) {
      throw new InvalidLicenseKeyException();
    }
  }

  private IssuedActivation issueActivationToken(MachineActivation activation) {
    GeneratedActivationToken generated = activationTokenGenerator.generate();
    activation.rotateActivationToken(generated.tokenHash());
    return new IssuedActivation(activation, generated.fullToken());
  }

  private MachineActivation requireActiveActivation(
      UUID productId, String activationToken, String machineFingerprint) {
    String tokenHash = sha256Hasher.hash(activationToken);
    MachineActivation activation =
        machineActivationRepository
            .findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash)
            .orElseThrow(InvalidActivationTokenException::new);

    boolean belongsToProduct = productId.equals(activation.getLicense().getProduct().getId());
    boolean matchesMachine =
        sha256Hasher.matches(machineFingerprint, activation.getMachineFingerprintHash());

    if (!belongsToProduct || !matchesMachine) {
      throw new InvalidActivationTokenException();
    }

    return activation;
  }
}
