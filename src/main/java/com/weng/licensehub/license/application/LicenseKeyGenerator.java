package com.weng.licensehub.license.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;


@Component
public class LicenseKeyGenerator {

    private static final int KEY_ID_BYTES = 8;
    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedLicenseKey generate() {

        byte[] keyIdBytes = generateRandomBytes(KEY_ID_BYTES);

        String keyId = HexFormat.of().withUpperCase().formatHex(keyIdBytes);

        byte[] secretBytes = generateRandomBytes(SECRET_BYTES);

        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String fullKey = "LH_" + keyId + "_" + secret;

        String keyHash = hashSecret(secret);

        return new GeneratedLicenseKey(keyId, keyHash, fullKey);
    }

    private byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes); // Fills the array with secure random bytes
        return bytes;
    }

    private String hashSecret(String secret) {
        try {
            byte[] secretUtf8 = secret.getBytes(StandardCharsets.UTF_8);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(secretUtf8);

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception);
        }

    }

}
