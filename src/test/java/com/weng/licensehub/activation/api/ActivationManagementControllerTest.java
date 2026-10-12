package com.weng.licensehub.activation.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.activation.application.ActivationManagementService;
import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.license.application.LicenseNotFoundException;
import com.weng.licensehub.license.domain.License;

@WebMvcTest(ActivationManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActivationManagementControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ActivationManagementService activationManagementService;

        @Test
        void findAllReturnsPagedSafeActivationHistory() throws Exception {
                UUID licenseId = UUID.randomUUID();
                UUID activationId = UUID.randomUUID();

                Instant activatedAt = Instant.parse("2026-10-01T10:00:00Z");
                Instant lastSeenAt = Instant.parse("2026-10-02T10:00:00Z");
                Instant deactivatedAt = Instant.parse("2026-10-03T10:00:00Z");

                License license = mock(License.class);
                MachineActivation activation = mock(MachineActivation.class);

                when(license.getId()).thenReturn(licenseId);
                when(activation.getId()).thenReturn(activationId);
                when(activation.getLicense()).thenReturn(license);
                when(activation.getMachineName())
                                .thenReturn("Development laptop");
                when(activation.getActivatedAt())
                                .thenReturn(activatedAt);
                when(activation.getLastSeenAt())
                                .thenReturn(lastSeenAt);
                when(activation.getDeactivatedAt())
                                .thenReturn(deactivatedAt);
                when(activation.isActive()).thenReturn(false);

                Pageable pageable = PageRequest.of(
                                0,
                                20,
                                Sort.Direction.DESC,
                                "activatedAt");

                Page<MachineActivation> page = new PageImpl<>(
                                List.of(activation),
                                pageable,
                                1);

                when(activationManagementService.findAllForOwner(
                                licenseId,
                                "owner@example.com",
                                pageable))
                                .thenReturn(page);

                mockMvc.perform(get(
                                "/api/licenses/{licenseId}/activations",
                                licenseId)
                                .principal(() -> "owner@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(activationId.toString()))
                                .andExpect(jsonPath("$.content[0].licenseId")
                                                .value(licenseId.toString()))
                                .andExpect(jsonPath("$.content[0].machineName")
                                                .value("Development laptop"))
                                .andExpect(jsonPath("$.content[0].activatedAt")
                                                .value(activatedAt.toString()))
                                .andExpect(jsonPath("$.content[0].lastSeenAt")
                                                .value(lastSeenAt.toString()))
                                .andExpect(jsonPath("$.content[0].deactivatedAt")
                                                .value(deactivatedAt.toString()))
                                .andExpect(jsonPath("$.content[0].active")
                                                .value(false))
                                .andExpect(jsonPath(
                                                "$.content[0].machineFingerprintHash")
                                                .doesNotExist())
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1));

                verify(activationManagementService).findAllForOwner(
                                licenseId,
                                "owner@example.com",
                                pageable);
        }

        @Test
        void findAllReturns404WhenLicenseIsNotOwned() throws Exception {
                UUID licenseId = UUID.randomUUID();

                Pageable pageable = PageRequest.of(
                                0,
                                20,
                                Sort.Direction.DESC,
                                "activatedAt");

                when(activationManagementService.findAllForOwner(
                                licenseId,
                                "owner@example.com",
                                pageable))
                                .thenThrow(new LicenseNotFoundException(licenseId));

                mockMvc.perform(get(
                                "/api/licenses/{licenseId}/activations",
                                licenseId)
                                .principal(() -> "owner@example.com"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title")
                                                .value("License not found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("License '" + licenseId + "' not found"))
                                .andExpect(jsonPath("$.instance")
                                                .value("/api/licenses/"
                                                                + licenseId
                                                                + "/activations"));

                verify(activationManagementService).findAllForOwner(
                                licenseId,
                                "owner@example.com",
                                pageable);
        }
}