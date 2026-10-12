package com.weng.licensehub.activation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.activation.persistence.MachineActivationRepository;
import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.license.application.LicenseService;
import com.weng.licensehub.license.domain.License;

@ExtendWith(MockitoExtension.class)
class ActivationManagementServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private LicenseService licenseService;

    @Mock
    private MachineActivationRepository activationRepository;

    private ActivationManagementService service;

    @BeforeEach
    void setUp() {
        service = new ActivationManagementService(
                licenseService,
                activationRepository);
    }

    @Test
    void findAllForOwnerReturnsRepositoryResults() {
        UUID licenseId = UUID.randomUUID();
        License license = mock(License.class);
        MachineActivation activation = mock(MachineActivation.class);
        Pageable pageable = PageRequest.of(0, 20);
        Page<MachineActivation> expected = new PageImpl<>(
                List.of(activation),
                pageable,
                1);

        when(licenseService.getByIdForOwner(
                licenseId,
                OWNER_EMAIL))
                .thenReturn(license);

        when(activationRepository.findAllByLicense_Id(
                licenseId,
                pageable))
                .thenReturn(expected);

        Page<MachineActivation> result = service.findAllForOwner(
                licenseId,
                OWNER_EMAIL,
                pageable);

        assertThat(result).isSameAs(expected);

        verify(licenseService).getByIdForOwner(
                licenseId,
                OWNER_EMAIL);

        verify(activationRepository).findAllByLicense_Id(
                licenseId,
                pageable);
    }

    @Test
    void findAllForOwnerDoesNotQueryActivationsWhenLicenseIsNotOwned() {
        UUID licenseId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        LicenseNotFoundException exception =
                new LicenseNotFoundException(licenseId);

        when(licenseService.getByIdForOwner(
                licenseId,
                OWNER_EMAIL))
                .thenThrow(exception);

        assertThatThrownBy(() -> service.findAllForOwner(
                licenseId,
                OWNER_EMAIL,
                pageable))
                .isSameAs(exception);

        verifyNoInteractions(activationRepository);
    }
}
