package com.weng.licensehub.activation.application;

import com.weng.licensehub.activation.domain.MachineActivation;

public record IssuedActivation(MachineActivation activation, String activationToken) {}
