package com.weng.licensehub.activation.api;

import com.weng.licensehub.activation.application.ActivationService;
import com.weng.licensehub.activation.application.IssuedActivation;
import com.weng.licensehub.activation.domain.MachineActivation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivationController {

  private final ActivationService activationService;

  public ActivationController(ActivationService service) {
    this.activationService = service;
  }

  @PostMapping("/activations")
  @ResponseStatus(HttpStatus.OK)
  public IssuedActivationResponse activate(@Valid @RequestBody ActivationMachineRequest request) {

    IssuedActivation issued =
        activationService.activate(
            request.productId(),
            request.licenseKey(),
            request.machineFingerprint(),
            request.machineName());

    MachineActivation activation = issued.activation();
    return new IssuedActivationResponse(
        activation.getId(),
        activation.getLicense().getId(),
        activation.getMachineName(),
        activation.getActivatedAt(),
        activation.getLastSeenAt(),
        issued.activationToken());
  }

  private ActivationResponse toResponse(MachineActivation activation) {
    return new ActivationResponse(
        activation.getId(),
        activation.getLicense().getId(),
        activation.getMachineName(),
        activation.getActivatedAt(),
        activation.getLastSeenAt());
  }

  @PostMapping("/activations/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@Valid @RequestBody DeactivateMachineRequest request) {

    activationService.deactivate(
        request.productId(), request.activationToken(), request.machineFingerprint());
  }

  @PostMapping("/activations/validate")
  public ActivationResponse validate(@Valid @RequestBody ValidateActivationRequest request) {
    MachineActivation activation =
        activationService.validate(
            request.productId(), request.activationToken(), request.machineFingerprint());

    return toResponse(activation);
  }
}
