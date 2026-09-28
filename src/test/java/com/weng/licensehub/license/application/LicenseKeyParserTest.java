package com.weng.licensehub.license.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.weng.licensehub.license.api.ParsedLicenseKey;

class LicenseKeyParserTest {

    private final LicenseKeyParser parser =
            new LicenseKeyParser();

    @Test
    void parsesValidLicenseKey() {
        String secret = "A".repeat(43);

        var result = parser.parse(
                "LH_0123456789ABCDEF_" + secret);

        assertThat(result).contains(
                new ParsedLicenseKey(
                        "0123456789ABCDEF",
                        secret));
    }

    @Test
    void rejectsMalformedLicenseKey() {
        var result = parser.parse("not-a-license-key");

        assertThat(result).isEmpty();
    }

    @Test
    void rejectsNullLicenseKey() {
        assertThat(parser.parse(null)).isEmpty();
    }
}