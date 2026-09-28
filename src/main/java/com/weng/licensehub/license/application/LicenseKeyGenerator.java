package com.weng.licensehub.license.application;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;
import com.weng.licensehub.shared.security.Sha256Hasher;

@Component
public class LicenseKeyGenerator {

    private static final int KEY_ID_BYTES = 8;
    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Sha256Hasher sha256Hasher;

public LicenseKeyGenerator(Sha256Hasher sha256Hasher) {
    this.sha256Hasher = sha256Hasher;
}



    public GeneratedLicenseKey generate() {

        byte[] keyIdBytes = generateRandomBytes(KEY_ID_BYTES);

        String keyId = HexFormat.of().withUpperCase().formatHex(keyIdBytes);

        byte[] secretBytes = generateRandomBytes(SECRET_BYTES);

        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String fullKey = "LH_" + keyId + "_" + secret;

        String keyHash = sha256Hasher.hash(secret);

        return new GeneratedLicenseKey(keyId, keyHash, fullKey);
    }

    private byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes); // Fills the array with secure random bytes
        return bytes;
    }



}
