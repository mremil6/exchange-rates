package com.marcura.exchange.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeRateOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marcura Exchange Rate API")
                        .description("Spread-adjusted exchange rates, historical trends, "
                                + "currency usage analytics, and AI-generated trend insight.")
                        .version("1.0.0")
                        .contact(new Contact().name("Marcura R&D"))
                        .license(new License().name("Internal").url("https://marcura.example")));
    }
}
