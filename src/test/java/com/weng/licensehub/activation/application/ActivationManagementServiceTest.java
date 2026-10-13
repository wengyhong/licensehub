package com.weng.licensehub.activation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ActivationManagementServiceTest {

  private static final String OWNER_EMAIL = "owner@example.com";

  @Mock private LicenseService licenseService;

  @Mock private MachineActivationRepository activationRepository;

  private ActivationManagementService service;
  private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

  @BeforeEach
  void setUp() {

    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    service = new ActivationManagementService(licenseService, activationRepository, clock);
  }

  @Test
  void findAllForOwnerReturnsRepositoryResults() {
    UUID licenseId = UUID.randomUUID();
    License license = mock(License.class);
    MachineActivation activation = mock(MachineActivation.class);
    Pageable pageable = PageRequest.of(0, 20);
    Page<MachineActivation> expected = new PageImpl<>(List.of(activation), pageable, 1);

    when(licenseService.getByIdForOwner(licenseId, OWNER_EMAIL)).thenReturn(license);

    when(activationRepository.findAllByLicense_Id(licenseId, pageable)).thenReturn(expected);

    Page<MachineActivation> result = service.findAllForOwner(licenseId, OWNER_EMAIL, pageable);

    assertThat(result).isSameAs(expected);

    verify(licenseService).getByIdForOwner(licenseId, OWNER_EMAIL);

    verify(activationRepository).findAllByLicense_Id(licenseId, pageable);
  }

  @Test
  void findAllForOwnerDoesNotQueryActivationsWhenLicenseIsNotOwned() {
    UUID licenseId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    LicenseNotFoundException exception = new LicenseNotFoundException(licenseId);

    when(licenseService.getByIdForOwner(licenseId, OWNER_EMAIL)).thenThrow(exception);

    assertThatThrownBy(() -> service.findAllForOwner(licenseId, OWNER_EMAIL, pageable))
        .isSameAs(exception);

    verifyNoInteractions(activationRepository);
  }

  @Test
  void deactivateForOwnerDeactivatesActivationAtCurrentTime() {
    UUID licenseId = UUID.randomUUID();
    UUID activationId = UUID.randomUUID();

    License license = mock(License.class);
    MachineActivation activation = mock(MachineActivation.class);

    when(licenseService.getByIdForOwner(licenseId, OWNER_EMAIL)).thenReturn(license);

    when(activationRepository.findByIdAndLicense_Id(activationId, licenseId))
        .thenReturn(Optional.of(activation));

    service.deactivateForOwner(licenseId, activationId, OWNER_EMAIL);

    verify(licenseService).getByIdForOwner(licenseId, OWNER_EMAIL);

    verify(activationRepository).findByIdAndLicense_Id(activationId, licenseId);

    verify(activation).deactivate(NOW);
  }

  @Test
  void deactivateForOwnerThrowsWhenActivationDoesNotBelongToLicense() {
    UUID licenseId = UUID.randomUUID();
    UUID activationId = UUID.randomUUID();

    License license = mock(License.class);

    when(licenseService.getByIdForOwner(licenseId, OWNER_EMAIL)).thenReturn(license);

    when(activationRepository.findByIdAndLicense_Id(activationId, licenseId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deactivateForOwner(licenseId, activationId, OWNER_EMAIL))
        .isInstanceOf(MachineActivationNotFoundException.class)
        .hasMessage("Machine activation '" + activationId + "' not found");
  }
}
