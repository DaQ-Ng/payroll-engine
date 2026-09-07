package com.payrollengine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payrollEngineOpenApi() {
        String schemeName = "ApiKeyAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Payroll Processing Engine")
                        .version("v1")
                        .description("Idempotent payroll runs, IRS-structured tax withholding, and a balanced "
                                + "double-entry ledger. See the README for the concurrency and idempotency design."))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
