package com.rightfull.config;

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
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "SupabaseBearerAuth";

    @Bean
    public OpenAPI rightfullOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rightfull API")
                        .description("Production-Grade Multi-Tenant SaaS Software Licensing Management Platform API")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("Rightfull Core Team")
                                .url("https://rightfull.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Supabase Auth Access Token (RS256/ES256 signed JWT)")));
    }
}
