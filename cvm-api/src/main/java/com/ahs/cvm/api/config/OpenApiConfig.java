package com.ahs.cvm.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cvmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CVE-Relevance-Manager API")
                        .description("REST-API des CVE-Relevance-Managers (CVM)")
                        .version("0.0.1"));
    }
}
