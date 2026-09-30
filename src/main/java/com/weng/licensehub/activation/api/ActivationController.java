package com.weng.licensehub.activation.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weng.licensehub.activation.application.ActivationService;

import com.weng.licensehub.activation.domain.MachineActivation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ActivationController {

    private final ActivationService activationService;

    public ActivationController(ActivationService service) {
        this.activationService = service;
    }

    @PostMapping("/activations")
    @ResponseStatus(HttpStatus.OK)
    public ActivationResponse activate(
            @Valid @RequestBody ActivationMachineRequest request

    ) {
        MachineActivation activation = activationService.activate(request.licenseKey(), request.machineFingerprint(),
                request.machineName());

        return (toResponse(activation));
    }

    private ActivationResponse toResponse(MachineActivation activation) {
        return new ActivationResponse(activation.getId(), activation.getLicense().getId(), activation.getMachineName(),
                activation.getActivatedAt(), activation.getLastSeenAt());
    }

    @PostMapping("/activations/deactivate")
    @ResponseStatus (HttpStatus.NO_CONTENT)
    public void deactivate(

        @Valid
        @RequestBody
        DeactivateMachineRequest request)
        {
            activationService.deactivate(request.licenseKey(), request.machineFingerprint());
        }
}
