package com.weng.licensehub.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "LicenseHub API",
            version = "v1",
            description =
                """
                        REST API for managing software products,
                        licenses, and machine activations.
                        """))
public class OpenApiConfiguration {}
