package com.weng.licensehub.license.api;

public record IssuedLicenseResponse(LicenseResponse license,
    String licenseKey
) {

}
