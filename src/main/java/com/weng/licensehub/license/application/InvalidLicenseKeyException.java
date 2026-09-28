package com.weng.licensehub.license.application;

public class InvalidLicenseKeyException extends RuntimeException{

    public InvalidLicenseKeyException(){
        super("Invalid license key");
    }

}
