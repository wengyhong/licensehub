package com.weng.licensehub.activation.application;

import java.util.UUID;

public class MachineActivationNotFoundException extends RuntimeException {
  public MachineActivationNotFoundException(UUID activationId) {
    super("Machine activation '" + activationId + "' not found");
  }
}
