package com.weng.licensehub.activation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.InvalidLicenseKeyException;
import com.weng.licensehub.license.application.LicenseKeyVerifier;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.product.domain.Product;
import com.weng.licensehub.shared.security.Sha256Hasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
  private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  @Mock private LicenseRepository licenseRepository;
  @Mock private LicenseKeyVerifier licenseKeyVerifier;

  @Mock private Sha256Hasher sha256Hasher;

  @Mock private MachineActivationRepository activationRepository;
  @Mock private ActivationTokenGenerator activationTokenGenerator;

  private ActivationService activationService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    activationService =
        new ActivationService(
            licenseKeyVerifier,
            sha256Hasher,
            activationRepository,
            clock,
            licenseRepository,
            activationTokenGenerator);
  }

  @Test
  void createsActivationForNewMachine() {
    String fullKey = "full-license-key";
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    String tokenHash = "a".repeat(64);
    String fullToken = "LA_generated-token";
    UUID licenseId = UUID.randomUUID();

    License license = licenseForProduct(PRODUCT_ID);

    when(license.getId()).thenReturn(licenseId);

    when(licenseRepository.findByIdForUpdate(licenseId)).thenReturn(Optional.of(license));

    when(licenseKeyVerifier.verify(fullKey)).thenReturn(license);

    when(license.canActivateAt(NOW)).thenReturn(true);

    when(license.getId()).thenReturn(licenseId);

    when(license.getMaxActivations()).thenReturn(3);

    when(sha256Hasher.hash(rawFingerprint)).thenReturn(fingerprintHash);

    when(activationRepository.findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
            licenseId, fingerprintHash))
        .thenReturn(Optional.empty());

    when(activationRepository.countByLicense_IdAndDeactivatedAtIsNull(licenseId)).thenReturn(1L);

    when(activationRepository.save(any(MachineActivation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(activationTokenGenerator.generate())
        .thenReturn(new GeneratedActivationToken(tokenHash, fullToken));

    IssuedActivation result =
        activationService.activate(PRODUCT_ID, fullKey, rawFingerprint, "Development laptop");

    ArgumentCaptor<MachineActivation> captor = ArgumentCaptor.forClass(MachineActivation.class);

    verify(activationRepository).save(captor.capture());

    MachineActivation saved = captor.getValue();

    assertThat(result.activation()).isSameAs(saved);
    assertThat(result.activationToken()).isEqualTo(fullToken);
    assertThat(saved.getLicense()).isSameAs(license);

    assertThat(saved.getMachineFingerprintHash())
        .isEqualTo(fingerprintHash)
        .isNotEqualTo(rawFingerprint);

    assertThat(saved.getMachineName()).isEqualTo("Development laptop");
    assertThat(saved.getActivationTokenHash()).isEqualTo(tokenHash);
  }

  @Test
  void refreshesExistingActivationWithoutUsingAnotherSlot() {
    String fullKey = "full-license-key";
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    String tokenHash = "a".repeat(64);
    String fullToken = "LA_rotated-token";
    UUID licenseId = UUID.randomUUID();

    License license = licenseForProduct(PRODUCT_ID);
    when(license.getId()).thenReturn(licenseId);

    when(licenseRepository.findByIdForUpdate(licenseId)).thenReturn(Optional.of(license));
    MachineActivation existingActivation = mock(MachineActivation.class);

    when(licenseKeyVerifier.verify(fullKey)).thenReturn(license);

    when(license.canActivateAt(NOW)).thenReturn(true);

    when(license.getId()).thenReturn(licenseId);

    when(sha256Hasher.hash(rawFingerprint)).thenReturn(fingerprintHash);

    when(activationRepository.findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
            licenseId, fingerprintHash))
        .thenReturn(Optional.of(existingActivation));
    when(activationTokenGenerator.generate())
        .thenReturn(new GeneratedActivationToken(tokenHash, fullToken));

    IssuedActivation result =
        activationService.activate(PRODUCT_ID, fullKey, rawFingerprint, "Development laptop");

    assertThat(result.activation()).isSameAs(existingActivation);
    assertThat(result.activationToken()).isEqualTo(fullToken);

    verify(existingActivation).markSeen(NOW);
    verify(existingActivation).rotateActivationToken(tokenHash);

    verify(activationRepository, never()).countByLicense_IdAndDeactivatedAtIsNull(licenseId);

    verify(activationRepository, never()).save(any(MachineActivation.class));
  }

  @Test
  void rejectsInactiveOrExpiredLicenseBeforeHashingFingerprint() {
    String fullKey = "full-license-key";
    License license = licenseForProduct(PRODUCT_ID);
    UUID licenseId = UUID.randomUUID();
    when(license.getId()).thenReturn(licenseId);

    when(licenseRepository.findByIdForUpdate(licenseId)).thenReturn(Optional.of(license));
    when(licenseKeyVerifier.verify(fullKey)).thenReturn(license);

    when(license.canActivateAt(NOW)).thenReturn(false);

    assertThatThrownBy(
            () ->
                activationService.activate(
                    PRODUCT_ID, fullKey, "raw-machine-fingerprint", "Development laptop"))
        .isInstanceOf(LicenseNotActivatableException.class)
        .hasMessage("License is inactive or expired");

    verifyNoInteractions(sha256Hasher, activationRepository);
  }

  @Test
  void rejectsNewMachineWhenActivationLimitIsReached() {
    String fullKey = "full-license-key";
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    UUID licenseId = UUID.randomUUID();

    License license = licenseForProduct(PRODUCT_ID);
    when(license.getId()).thenReturn(licenseId);

    when(licenseRepository.findByIdForUpdate(licenseId)).thenReturn(Optional.of(license));
    when(licenseKeyVerifier.verify(fullKey)).thenReturn(license);

    when(license.canActivateAt(NOW)).thenReturn(true);

    when(license.getId()).thenReturn(licenseId);

    when(license.getMaxActivations()).thenReturn(2);

    when(sha256Hasher.hash(rawFingerprint)).thenReturn(fingerprintHash);

    when(activationRepository.findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
            licenseId, fingerprintHash))
        .thenReturn(Optional.empty());

    when(activationRepository.countByLicense_IdAndDeactivatedAtIsNull(licenseId)).thenReturn(2L);

    assertThatThrownBy(
            () ->
                activationService.activate(
                    PRODUCT_ID, fullKey, rawFingerprint, "Development laptop"))
        .isInstanceOf(ActivationLimitExceededException.class)
        .hasMessage("Maximum number of activations reached");

    verify(activationRepository, never()).save(any(MachineActivation.class));
  }

  @Test
  void deactivatesMachineWithValidActivationToken() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(PRODUCT_ID);
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);
    when(sha256Hasher.matches(rawFingerprint, fingerprintHash)).thenReturn(true);

    activationService.deactivate(PRODUCT_ID, activationToken, rawFingerprint);

    verify(activation).deactivate(NOW);
    verifyNoInteractions(licenseKeyVerifier, licenseRepository);
  }

  @Test
  void deactivateRejectsInvalidActivationToken() {
    String activationToken = "LA_invalid-token";
    String tokenHash = "t".repeat(64);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                activationService.deactivate(
                    PRODUCT_ID, activationToken, "raw-machine-fingerprint"))
        .isInstanceOf(InvalidActivationTokenException.class)
        .hasMessage("Invalid activation token");
  }

  @Test
  void validateRefreshesActiveMachine() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(PRODUCT_ID);
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);
    when(sha256Hasher.matches(rawFingerprint, fingerprintHash)).thenReturn(true);
    when(license.canActivateAt(NOW)).thenReturn(true);

    MachineActivation result =
        activationService.validate(PRODUCT_ID, activationToken, rawFingerprint);

    assertThat(result).isSameAs(activation);
    verify(activation).markSeen(NOW);
    verifyNoInteractions(licenseKeyVerifier, licenseRepository);
  }

  @Test
  void validateRejectsInactiveOrExpiredLicense() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(PRODUCT_ID);
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);
    when(sha256Hasher.matches(rawFingerprint, fingerprintHash)).thenReturn(true);
    when(license.canActivateAt(NOW)).thenReturn(false);

    assertThatThrownBy(
            () -> activationService.validate(PRODUCT_ID, activationToken, rawFingerprint))
        .isInstanceOf(LicenseNotActivatableException.class)
        .hasMessage("License is inactive or expired");

    verify(activation, never()).markSeen(any(Instant.class));
  }

  @Test
  void validateRejectsInvalidActivationToken() {
    String activationToken = "LA_invalid-token";
    String tokenHash = "t".repeat(64);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                activationService.validate(PRODUCT_ID, activationToken, "raw-machine-fingerprint"))
        .isInstanceOf(InvalidActivationTokenException.class)
        .hasMessage("Invalid activation token");
  }

  @Test
  void validateRejectsTokenUsedFromDifferentMachine() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(PRODUCT_ID);
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);
    when(sha256Hasher.matches("different-machine", fingerprintHash)).thenReturn(false);

    assertThatThrownBy(
            () -> activationService.validate(PRODUCT_ID, activationToken, "different-machine"))
        .isInstanceOf(InvalidActivationTokenException.class)
        .hasMessage("Invalid activation token");

    verify(license, never()).canActivateAt(any(Instant.class));
  }

  @Test
  void rejectsLicenseBelongingToAnotherProductBeforeHashingFingerprint() {
    String fullKey = "full-license-key";
    UUID licenseId = UUID.randomUUID();
    License license = licenseForProduct(UUID.randomUUID());

    when(license.getId()).thenReturn(licenseId);
    when(licenseKeyVerifier.verify(fullKey)).thenReturn(license);
    when(licenseRepository.findByIdForUpdate(licenseId)).thenReturn(Optional.of(license));

    assertThatThrownBy(
            () ->
                activationService.activate(
                    PRODUCT_ID, fullKey, "raw-machine-fingerprint", "Development laptop"))
        .isInstanceOf(InvalidLicenseKeyException.class)
        .hasMessage("Invalid license key");

    verifyNoInteractions(sha256Hasher, activationRepository);
  }

  @Test
  void deactivateRejectsTokenBelongingToAnotherProduct() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(UUID.randomUUID());
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);

    assertThatThrownBy(
            () ->
                activationService.deactivate(
                    PRODUCT_ID, activationToken, "raw-machine-fingerprint"))
        .isInstanceOf(InvalidActivationTokenException.class)
        .hasMessage("Invalid activation token");

    verify(activation, never()).deactivate(any(Instant.class));
  }

  @Test
  void validateRejectsTokenBelongingToAnotherProduct() {
    String activationToken = "LA_activation-token";
    String tokenHash = "t".repeat(64);
    String fingerprintHash = "f".repeat(64);
    License license = licenseForProduct(UUID.randomUUID());
    MachineActivation activation = mock(MachineActivation.class);

    when(sha256Hasher.hash(activationToken)).thenReturn(tokenHash);
    when(activationRepository.findByActivationTokenHashAndDeactivatedAtIsNull(tokenHash))
        .thenReturn(Optional.of(activation));
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineFingerprintHash()).thenReturn(fingerprintHash);

    assertThatThrownBy(
            () ->
                activationService.validate(PRODUCT_ID, activationToken, "raw-machine-fingerprint"))
        .isInstanceOf(InvalidActivationTokenException.class)
        .hasMessage("Invalid activation token");

    verify(license, never()).canActivateAt(any(Instant.class));
  }

  private License licenseForProduct(UUID productId) {
    Product product = mock(Product.class);
    License license = mock(License.class);

    when(product.getId()).thenReturn(productId);
    when(license.getProduct()).thenReturn(product);

    return license;
  }
}
