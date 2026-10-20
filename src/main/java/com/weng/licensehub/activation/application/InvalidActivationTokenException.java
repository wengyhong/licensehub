package com.weng.licensehub.activation.application;

public class InvalidActivationTokenException extends RuntimeException {

  public InvalidActivationTokenException() {
    super("Invalid activation token");
  }
}
