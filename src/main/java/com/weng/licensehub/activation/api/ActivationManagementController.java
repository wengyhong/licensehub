package com.weng.licensehub.activation.api;

import com.weng.licensehub.activation.application.ActivationManagementService;
import com.weng.licensehub.activation.domain.MachineActivation;
import com.weng.licensehub.shared.api.PagedResponse;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/licenses")
@RestController
public class ActivationManagementController {

  private final ActivationManagementService activationManagementService;

  public ActivationManagementController(ActivationManagementService activationManagementService) {
    this.activationManagementService = activationManagementService;
  }

  @GetMapping("/{licenseId}/activations")
  public PagedResponse<MachineActivationResponse> findAllByLicenseId(
      @PathVariable UUID licenseId,
      Principal principal,
      @PageableDefault(size = 20, sort = "activatedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<MachineActivationResponse> result =
        activationManagementService
            .findAllForOwner(licenseId, principal.getName(), pageable)
            .map(this::toResponse);

    return PagedResponse.from(result);
  }

  private MachineActivationResponse toResponse(MachineActivation activation) {

    return new MachineActivationResponse(
        activation.getId(),
        activation.getLicense().getId(),
        activation.getMachineName(),
        activation.getActivatedAt(),
        activation.getLastSeenAt(),
        activation.getDeactivatedAt(),
        activation.isActive());
  }

  @PostMapping("/{licenseId}/activations/{activationId}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivateForOwner(
      @PathVariable UUID licenseId, @PathVariable UUID activationId, Principal principal) {
    activationManagementService.deactivateForOwner(licenseId, activationId, principal.getName());
  }
}
