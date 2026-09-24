package com.weng.licensehub.license.application;

import com.weng.licensehub.license.domain.License;

public record IssuedLicense(
    License license,
    String fullKey
) {

}
