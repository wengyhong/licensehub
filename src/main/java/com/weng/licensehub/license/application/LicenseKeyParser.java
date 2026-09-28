package com.weng.licensehub.license.application;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.weng.licensehub.license.api.ParsedLicenseKey;

@Component
public class LicenseKeyParser {

    private static final Pattern KEY_PATTERN = Pattern.compile(            "LH_([0-9A-F]{16})_([A-Za-z0-9_-]{43})");


    public Optional<ParsedLicenseKey>parse(String fullkey)
    {
        if(fullkey == null)
        {
            return Optional.empty();
        }

        Matcher matcher = KEY_PATTERN.matcher(fullkey);

        if(!matcher.matches())
            return Optional.empty();

        return Optional.of(new ParsedLicenseKey(matcher.group(1), matcher.group(2)));
    }

}
