package com.weng.licensehub.activation.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.weng.licensehub.activation.application.ActivationService;
import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.license.domain.License;
import org.springframework.test.web.servlet.ResultActions;

import com.weng.licensehub.activation.application.ActivationLimitExceededException;
import com.weng.licensehub.activation.application.LicenseNotActivatableException;
import com.weng.licensehub.license.application.InvalidLicenseKeyException;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ActivationController.class)
class ActivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivationService activationService;

    @Test
    void activateReturns200AndSafeResponse() throws Exception {
        UUID activationId = UUID.randomUUID();
        UUID licenseId = UUID.randomUUID();

        Instant activatedAt = Instant.parse("2026-09-28T10:00:00Z");

        Instant lastSeenAt = Instant.parse("2026-09-28T10:05:00Z");

        License license = mock(License.class);

        MachineActivation activation = mock(MachineActivation.class);

        when(license.getId())
                .thenReturn(licenseId);

        when(activation.getId())
                .thenReturn(activationId);

        when(activation.getLicense())
                .thenReturn(license);

        when(activation.getMachineName())
                .thenReturn("Development laptop");

        when(activation.getActivatedAt())
                .thenReturn(activatedAt);

        when(activation.getLastSeenAt())
                .thenReturn(lastSeenAt);

        when(activationService.activate(
                "full-license-key",
                "raw-machine-fingerprint",
                "Development laptop"))
                .thenReturn(activation);

        mockMvc.perform(post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "licenseKey": "full-license-key",
                          "machineFingerprint": "raw-machine-fingerprint",
                          "machineName": "Development laptop"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(activationId.toString()))
                .andExpect(jsonPath("$.licenseId")
                        .value(licenseId.toString()))
                .andExpect(jsonPath("$.machineName")
                        .value("Development laptop"))
                .andExpect(jsonPath("$.activatedAt")
                        .value(activatedAt.toString()))
                .andExpect(jsonPath("$.lastSeenAt")
                        .value(lastSeenAt.toString()))
                .andExpect(jsonPath("$.licenseKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.machineFingerprint")
                        .doesNotExist())
                .andExpect(jsonPath("$.machineFingerprintHash")
                        .doesNotExist());

        verify(activationService).activate(
                "full-license-key",
                "raw-machine-fingerprint",
                "Development laptop");
    }

    @Test
    void activateReturns400ForBlankRequiredFields()
            throws Exception {

        mockMvc.perform(post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "licenseKey": "",
                          "machineFingerprint": " ",
                          "machineName": "Development laptop"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(activationService);
    }

    @Test
    void activateReturns401ForInvalidLicenseKey()
            throws Exception {

        when(activationService.activate(
                "full-license-key",
                "raw-machine-fingerprint",
                "Development laptop"))
                .thenThrow(new InvalidLicenseKeyException());

        performValidActivationRequest()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title")
                        .value("Invalid license key"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid license key"));
    }

    @Test
    void activateReturns403ForInactiveOrExpiredLicense()
            throws Exception {

        when(activationService.activate(
                "full-license-key",
                "raw-machine-fingerprint",
                "Development laptop"))
                .thenThrow(
                        new LicenseNotActivatableException());

        performValidActivationRequest()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title")
                        .value("License not activatable"))
                .andExpect(jsonPath("$.status")
                        .value(403))
                .andExpect(jsonPath("$.detail")
                        .value("License is inactive or expired"));
    }

    @Test
    void activateReturns409WhenActivationLimitIsReached()
            throws Exception {

        when(activationService.activate(
                "full-license-key",
                "raw-machine-fingerprint",
                "Development laptop"))
                .thenThrow(
                        new ActivationLimitExceededException());

        performValidActivationRequest()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Activation limit exceeded"))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "Maximum number of activations reached"));
    }

    private ResultActions performValidActivationRequest()
            throws Exception {

        return mockMvc.perform(post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "licenseKey": "full-license-key",
                          "machineFingerprint": "raw-machine-fingerprint",
                          "machineName": "Development laptop"
                        }
                        """));
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc.perform(post("/api/activations/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "licenseKey": "full-license-key",
                          "machineFingerprint": "raw-machine-fingerprint"
                        }
                        """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(activationService).deactivate(
                "full-license-key",
                "raw-machine-fingerprint");
    }

    @Test
    void deactivateReturns401ForInvalidLicenseKey()
            throws Exception {

        doThrow(new InvalidLicenseKeyException())
                .when(activationService)
                .deactivate(
                        "full-license-key",
                        "raw-machine-fingerprint");

        mockMvc.perform(post("/api/activations/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "licenseKey": "full-license-key",
                          "machineFingerprint": "raw-machine-fingerprint"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title")
                        .value("Invalid license key"))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.detail")
                        .value("Invalid license key"));
    }
}