package com.weng.licensehub.activation.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weng.licensehub.activation.application.ActivationLimitExceededException;
import com.weng.licensehub.activation.application.ActivationService;
import com.weng.licensehub.activation.application.InvalidActivationTokenException;
import com.weng.licensehub.activation.application.IssuedActivation;
import com.weng.licensehub.activation.application.LicenseNotActivatableException;
import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.license.application.InvalidLicenseKeyException;
import com.weng.licensehub.license.domain.License;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ActivationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActivationControllerTest {

  private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ActivationService activationService;

  @Test
  void activateReturns200AndSafeResponse() throws Exception {
    UUID activationId = UUID.randomUUID();
    UUID licenseId = UUID.randomUUID();

    Instant activatedAt = Instant.parse("2026-09-28T10:00:00Z");

    Instant lastSeenAt = Instant.parse("2026-09-28T10:05:00Z");

    License license = mock(License.class);

    MachineActivation activation = mock(MachineActivation.class);

    when(license.getId()).thenReturn(licenseId);

    when(activation.getId()).thenReturn(activationId);

    when(activation.getLicense()).thenReturn(license);

    when(activation.getMachineName()).thenReturn("Development laptop");

    when(activation.getActivatedAt()).thenReturn(activatedAt);

    when(activation.getLastSeenAt()).thenReturn(lastSeenAt);

    when(activationService.activate(
            PRODUCT_ID, "full-license-key", "raw-machine-fingerprint", "Development laptop"))
        .thenReturn(new IssuedActivation(activation, "LA_activation-token"));

    mockMvc
        .perform(
            post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "licenseKey": "full-license-key",
                                                  "machineFingerprint": "raw-machine-fingerprint",
                                                  "machineName": "Development laptop"
                                                }
                                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(activationId.toString()))
        .andExpect(jsonPath("$.licenseId").value(licenseId.toString()))
        .andExpect(jsonPath("$.machineName").value("Development laptop"))
        .andExpect(jsonPath("$.activatedAt").value(activatedAt.toString()))
        .andExpect(jsonPath("$.lastSeenAt").value(lastSeenAt.toString()))
        .andExpect(jsonPath("$.activationToken").value("LA_activation-token"))
        .andExpect(jsonPath("$.licenseKey").doesNotExist())
        .andExpect(jsonPath("$.machineFingerprint").doesNotExist())
        .andExpect(jsonPath("$.machineFingerprintHash").doesNotExist());

    verify(activationService)
        .activate(PRODUCT_ID, "full-license-key", "raw-machine-fingerprint", "Development laptop");
  }

  @Test
  void activateReturns400ForBlankRequiredFields() throws Exception {

    mockMvc
        .perform(
            post("/api/activations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "licenseKey": "",
                                                  "machineFingerprint": " ",
                                                  "machineName": "Development laptop"
                                                }
                                                """))
        .andExpect(status().isBadRequest())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
        .andExpect(jsonPath("$.errors.productId").exists())
        .andExpect(jsonPath("$.errors.licenseKey").exists())
        .andExpect(jsonPath("$.errors.machineFingerprint").exists());

    verifyNoInteractions(activationService);
  }

  @Test
  void activateReturns401ForInvalidLicenseKey() throws Exception {

    when(activationService.activate(
            PRODUCT_ID, "full-license-key", "raw-machine-fingerprint", "Development laptop"))
        .thenThrow(new InvalidLicenseKeyException());

    performValidActivationRequest()
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid license key"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Invalid license key"));
  }

  @Test
  void activateReturns403ForInactiveOrExpiredLicense() throws Exception {

    when(activationService.activate(
            PRODUCT_ID, "full-license-key", "raw-machine-fingerprint", "Development laptop"))
        .thenThrow(new LicenseNotActivatableException());

    performValidActivationRequest()
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("License not activatable"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value("License is inactive or expired"));
  }

  @Test
  void activateReturns409WhenActivationLimitIsReached() throws Exception {

    when(activationService.activate(
            PRODUCT_ID, "full-license-key", "raw-machine-fingerprint", "Development laptop"))
        .thenThrow(new ActivationLimitExceededException());

    performValidActivationRequest()
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Activation limit exceeded"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("Maximum number of activations reached"));
  }

  private ResultActions performValidActivationRequest() throws Exception {

    return mockMvc.perform(
        post("/api/activations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                                                {
                                              "productId": "11111111-1111-1111-1111-111111111111",
                                                  "licenseKey": "full-license-key",
                                                  "machineFingerprint": "raw-machine-fingerprint",
                                                  "machineName": "Development laptop"
                                                }
                                                """));
  }

  @Test
  void deactivateReturns204() throws Exception {
    mockMvc
        .perform(
            post("/api/activations/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "activationToken": "LA_activation-token",
                                                  "machineFingerprint": "raw-machine-fingerprint"
                                                }
                                                """))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(activationService)
        .deactivate(PRODUCT_ID, "LA_activation-token", "raw-machine-fingerprint");
  }

  @Test
  void deactivateReturns401ForInvalidActivationToken() throws Exception {

    doThrow(new InvalidActivationTokenException())
        .when(activationService)
        .deactivate(PRODUCT_ID, "LA_activation-token", "raw-machine-fingerprint");

    mockMvc
        .perform(
            post("/api/activations/deactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "activationToken": "LA_activation-token",
                                                  "machineFingerprint": "raw-machine-fingerprint"
                                                }
                                                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid activation token"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Invalid activation token"));
  }

  @Test
  void validateReturns200AndRefreshesActivation() throws Exception {
    UUID activationId = UUID.randomUUID();
    UUID licenseId = UUID.randomUUID();
    Instant lastSeenAt = Instant.parse("2026-10-12T10:00:00Z");

    License license = mock(License.class);
    MachineActivation activation = mock(MachineActivation.class);

    when(license.getId()).thenReturn(licenseId);
    when(activation.getId()).thenReturn(activationId);
    when(activation.getLicense()).thenReturn(license);
    when(activation.getMachineName()).thenReturn("Development laptop");
    when(activation.getActivatedAt()).thenReturn(Instant.parse("2026-10-01T10:00:00Z"));
    when(activation.getLastSeenAt()).thenReturn(lastSeenAt);

    when(activationService.validate(PRODUCT_ID, "LA_activation-token", "raw-machine-fingerprint"))
        .thenReturn(activation);

    mockMvc
        .perform(
            post("/api/activations/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "activationToken": "LA_activation-token",
                                                  "machineFingerprint": "raw-machine-fingerprint"
                                                }
                                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(activationId.toString()))
        .andExpect(jsonPath("$.licenseId").value(licenseId.toString()))
        .andExpect(jsonPath("$.lastSeenAt").value(lastSeenAt.toString()))
        .andExpect(jsonPath("$.licenseKey").doesNotExist())
        .andExpect(jsonPath("$.machineFingerprint").doesNotExist())
        .andExpect(jsonPath("$.machineFingerprintHash").doesNotExist());

    verify(activationService)
        .validate(PRODUCT_ID, "LA_activation-token", "raw-machine-fingerprint");
  }

  @Test
  void validateReturns401ForInvalidActivationToken() throws Exception {

    when(activationService.validate(PRODUCT_ID, "LA_activation-token", "raw-machine-fingerprint"))
        .thenThrow(new InvalidActivationTokenException());

    mockMvc
        .perform(
            post("/api/activations/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "productId": "11111111-1111-1111-1111-111111111111",
                                                  "activationToken": "LA_activation-token",
                                                  "machineFingerprint": "raw-machine-fingerprint"
                                                }
                                                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid activation token"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Invalid activation token"));
  }

  @Test
  void validateReturns400ForBlankRequiredFields() throws Exception {

    mockMvc
        .perform(
            post("/api/activations/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "activationToken": "",
                                                  "machineFingerprint": " "
                                                }
                                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.productId").exists())
        .andExpect(jsonPath("$.errors.activationToken").exists())
        .andExpect(jsonPath("$.errors.machineFingerprint").exists());

    verifyNoInteractions(activationService);
  }
}
