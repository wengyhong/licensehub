package com.weng.licensehub.license.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.shared.security.Sha256Hasher;

class LicenseKeyGeneratorTest {

    private final LicenseKeyGenerator generator = new LicenseKeyGenerator(new Sha256Hasher());

    @Test
    void generateReturnDifferentKeys() {
        GeneratedLicenseKey first = generator.generate();
        GeneratedLicenseKey second = generator.generate();

        assertNotEquals(first.keyId(), second.keyId());
        assertNotEquals(first.fullKey(), second.fullKey());
        assertNotEquals(first.keyHash(), second.keyHash());

    }

    @Test
    void GenerateKey()  throws NoSuchAlgorithmException  {
        GeneratedLicenseKey result = generator.generate();

        assertTrue(result.keyId().matches("[0-9A-F]{16}"));
        assertTrue(result.keyHash().matches("[0-9a-f]{64}"));
        assertTrue(result.fullKey().matches(
                "LH_[0-9A-F]{16}_[A-Za-z0-9_-]{43}"));

        String[] parts = result.fullKey().split("_", 3);

        assertEquals("LH", parts[0]);
        assertEquals(result.keyId(), parts[1]);
        assertEquals(result.keyHash(), hashKey(parts[2]));

    }

    private String hashKey(String secret) throws NoSuchAlgorithmException {

        byte[] secretByte = secret.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(secretByte);
        return HexFormat.of().formatHex(hash);
    }
}
