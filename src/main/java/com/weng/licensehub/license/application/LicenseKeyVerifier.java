package com.weng.licensehub.license.application;

import org.springframework.stereotype.Component;

import com.weng.licensehub.license.api.ParsedLicenseKey;
import com.weng.licensehub.license.domain.License;
import com.weng.licensehub.license.persistence.LicenseRepository;
import com.weng.licensehub.shared.security.Sha256Hasher;

@Component
public class LicenseKeyVerifier {

    private final Sha256Hasher sha256Hasher;
    private final LicenseKeyParser licenseKeyParser;
    private final LicenseRepository licenseRepository;

    public LicenseKeyVerifier(Sha256Hasher hasher, LicenseKeyParser parser, LicenseRepository repo) {
        this.sha256Hasher = hasher;
        this.licenseKeyParser = parser;
        this.licenseRepository = repo;

    }

    public License verify(String fullkey) {
        ParsedLicenseKey parsedLicenseKey = licenseKeyParser.parse(fullkey)
                .orElseThrow(() -> new InvalidLicenseKeyException());

        String parsedSercret = parsedLicenseKey.secret();
        String parsedKeyId = parsedLicenseKey.keyId();

        License loaded = licenseRepository.findByKeyId(parsedKeyId).orElseThrow(() -> new InvalidLicenseKeyException());

        if (!sha256Hasher.matches(parsedSercret, loaded.getKeyHash())) {
            throw new InvalidLicenseKeyException();
        }

        return loaded;
    }
}
