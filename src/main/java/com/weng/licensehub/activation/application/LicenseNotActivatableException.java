package com.weng.licensehub.activation.application;

public class LicenseNotActivatableException extends RuntimeException {

    public LicenseNotActivatableException(){
    super("License is inactive or expired");
    }
}
