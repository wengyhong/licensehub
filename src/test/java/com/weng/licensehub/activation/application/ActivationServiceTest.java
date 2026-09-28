package com.weng.licensehub.activation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.LicenseKeyVerifier;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.shared.security.Sha256Hasher;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private LicenseKeyVerifier licenseKeyVerifier;

    @Mock
    private Sha256Hasher sha256Hasher;

    @Mock
    private MachineActivationRepository activationRepository;

    private ActivationService activationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        activationService = new ActivationService(
                licenseKeyVerifier,
                sha256Hasher,
                activationRepository,
                clock, licenseRepository);
    }

    @Test
    void createsActivationForNewMachine() {
        String fullKey = "full-license-key";
        String rawFingerprint = "raw-machine-fingerprint";
        String fingerprintHash = "f".repeat(64);
        UUID licenseId = UUID.randomUUID();

        License license = mock(License.class);

        when(license.getId())
                .thenReturn(licenseId);

        when(licenseRepository.findByIdForUpdate(licenseId))
                .thenReturn(Optional.of(license));

        when(licenseKeyVerifier.verify(fullKey))
                .thenReturn(license);

        when(license.canActivateAt(NOW))
                .thenReturn(true);

        when(license.getId())
                .thenReturn(licenseId);

        when(license.getMaxActivations())
                .thenReturn(3);

        when(sha256Hasher.hash(rawFingerprint))
                .thenReturn(fingerprintHash);

        when(activationRepository
                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                        licenseId,
                        fingerprintHash))
                .thenReturn(Optional.empty());

        when(activationRepository
                .countByLicense_IdAndDeactivatedAtIsNull(
                        licenseId))
                .thenReturn(1L);

        when(activationRepository.save(
                any(MachineActivation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MachineActivation result = activationService.activate(
                fullKey,
                rawFingerprint,
                "Development laptop");

        ArgumentCaptor<MachineActivation> captor = ArgumentCaptor.forClass(
                MachineActivation.class);

        verify(activationRepository)
                .save(captor.capture());

        MachineActivation saved = captor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getLicense()).isSameAs(license);

        assertThat(saved.getMachineFingerprintHash())
                .isEqualTo(fingerprintHash)
                .isNotEqualTo(rawFingerprint);

        assertThat(saved.getMachineName())
                .isEqualTo("Development laptop");
    }

    @Test
    void refreshesExistingActivationWithoutUsingAnotherSlot() {
        String fullKey = "full-license-key";
        String rawFingerprint = "raw-machine-fingerprint";
        String fingerprintHash = "f".repeat(64);
        UUID licenseId = UUID.randomUUID();

        License license = mock(License.class);
        when(license.getId())
                .thenReturn(licenseId);

        when(licenseRepository.findByIdForUpdate(licenseId))
                .thenReturn(Optional.of(license));
        MachineActivation existingActivation = mock(MachineActivation.class);

        when(licenseKeyVerifier.verify(fullKey))
                .thenReturn(license);

        when(license.canActivateAt(NOW))
                .thenReturn(true);

        when(license.getId())
                .thenReturn(licenseId);

        when(sha256Hasher.hash(rawFingerprint))
                .thenReturn(fingerprintHash);

        when(activationRepository
                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                        licenseId,
                        fingerprintHash))
                .thenReturn(Optional.of(existingActivation));

        MachineActivation result = activationService.activate(
                fullKey,
                rawFingerprint,
                "Development laptop");

        assertThat(result).isSameAs(existingActivation);

        verify(existingActivation).markSeen(NOW);

        verify(activationRepository, never())
                .countByLicense_IdAndDeactivatedAtIsNull(
                        licenseId);

        verify(activationRepository, never())
                .save(any(MachineActivation.class));
    }

    @Test
    void rejectsInactiveOrExpiredLicenseBeforeHashingFingerprint() {
        String fullKey = "full-license-key";
        License license = mock(License.class);
        UUID licenseId = UUID.randomUUID();
        when(license.getId())
                .thenReturn(licenseId);

        when(licenseRepository.findByIdForUpdate(licenseId))
                .thenReturn(Optional.of(license));
        when(licenseKeyVerifier.verify(fullKey))
                .thenReturn(license);

        when(license.canActivateAt(NOW))
                .thenReturn(false);

        assertThatThrownBy(() -> activationService.activate(
                fullKey,
                "raw-machine-fingerprint",
                "Development laptop"))
                .isInstanceOf(
                        LicenseNotActivatableException.class)
                .hasMessage("License is inactive or expired");

        verifyNoInteractions(
                sha256Hasher,
                activationRepository);
    }

    @Test
    void rejectsNewMachineWhenActivationLimitIsReached() {
        String fullKey = "full-license-key";
        String rawFingerprint = "raw-machine-fingerprint";
        String fingerprintHash = "f".repeat(64);
        UUID licenseId = UUID.randomUUID();

        License license = mock(License.class);
        when(license.getId())
                .thenReturn(licenseId);

        when(licenseRepository.findByIdForUpdate(licenseId))
                .thenReturn(Optional.of(license));
        when(licenseKeyVerifier.verify(fullKey))
                .thenReturn(license);

        when(license.canActivateAt(NOW))
                .thenReturn(true);

        when(license.getId())
                .thenReturn(licenseId);

        when(license.getMaxActivations())
                .thenReturn(2);

        when(sha256Hasher.hash(rawFingerprint))
                .thenReturn(fingerprintHash);

        when(activationRepository
                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                        licenseId,
                        fingerprintHash))
                .thenReturn(Optional.empty());

        when(activationRepository
                .countByLicense_IdAndDeactivatedAtIsNull(
                        licenseId))
                .thenReturn(2L);

        assertThatThrownBy(() -> activationService.activate(
                fullKey,
                rawFingerprint,
                "Development laptop"))
                .isInstanceOf(
                        ActivationLimitExceededException.class)
                .hasMessage(
                        "Maximum number of activations reached");

        verify(activationRepository, never())
                .save(any(MachineActivation.class));
    }

    @Test
    void deactivatesExistingMachine() {
        String fullKey = "full-license-key";
        String rawFingerprint = "raw-machine-fingerprint";
        String fingerprintHash = "f".repeat(64);
        UUID licenseId = UUID.randomUUID();

        License license = mock(License.class);

        MachineActivation activation = mock(MachineActivation.class);

        when(licenseKeyVerifier.verify(fullKey))
                .thenReturn(license);

        when(license.getId())
                .thenReturn(licenseId);

        when(licenseRepository.findByIdForUpdate(licenseId))
                .thenReturn(Optional.of(license));

        when(sha256Hasher.hash(rawFingerprint))
                .thenReturn(fingerprintHash);

        when(activationRepository
                .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                        licenseId,
                        fingerprintHash))
                .thenReturn(Optional.of(activation));

        activationService.deactivate(
                fullKey,
                rawFingerprint);

        verify(activation).deactivate(NOW);

        verify(license, never())
                .canActivateAt(any(Instant.class));

        verify(activationRepository, never())
                .save(any(MachineActivation.class));
    }

    @Test
void deactivatingMachineWithoutActiveActivationIsIdempotent() {
    String fullKey = "full-license-key";
    String rawFingerprint = "raw-machine-fingerprint";
    String fingerprintHash = "f".repeat(64);
    UUID licenseId = UUID.randomUUID();

    License license = mock(License.class);

    when(licenseKeyVerifier.verify(fullKey))
            .thenReturn(license);

    when(license.getId())
            .thenReturn(licenseId);

    when(licenseRepository.findByIdForUpdate(licenseId))
            .thenReturn(Optional.of(license));

    when(sha256Hasher.hash(rawFingerprint))
            .thenReturn(fingerprintHash);

    when(activationRepository
            .findByLicense_IdAndMachineFingerprintHashAndDeactivatedAtIsNull(
                    licenseId,
                    fingerprintHash))
            .thenReturn(Optional.empty());

    assertThatCode(() ->
            activationService.deactivate(
                    fullKey,
                    rawFingerprint))
            .doesNotThrowAnyException();

    verify(license, never())
            .canActivateAt(any(Instant.class));

    verify(activationRepository, never())
            .countByLicense_IdAndDeactivatedAtIsNull(
                    licenseId);

    verify(activationRepository, never())
            .save(any(MachineActivation.class));
}
}