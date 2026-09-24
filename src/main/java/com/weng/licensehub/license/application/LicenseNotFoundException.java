package com.weng.licensehub.license.application;

import java.util.UUID;

public class LicenseNotFoundException extends RuntimeException {
    public LicenseNotFoundException(UUID licenseId)
    {
        super(String.format("License '%s' not found", licenseId));
    }
}
