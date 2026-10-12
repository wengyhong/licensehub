package com.weng.licensehub.shared.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "LicenseHub API",
                version = "v1",
                description = """
                        REST API for managing software products,
                        licenses, and machine activations.
                        """))
public class OpenApiConfiguration {
}