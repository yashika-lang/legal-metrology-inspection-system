package com.legalmetrology.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI legalMetrologyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Legal Metrology AI Inspection System API")
                        .description("REST API for the Government of India Legal Metrology inspection platform — "
                                + "authentication, inspections, products, scans, images, reports, history and settings.")
                        .version("v1")
                        .contact(new Contact().name("Legal Metrology Engineering"))
                        .license(new License().name("Proprietary — Government of India")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /api/v1/auth/login")));
    }
}
