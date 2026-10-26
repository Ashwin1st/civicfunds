package com.civicfunds.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI civicFundsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CivicFunds API")
                .version("0.1.0")
                .description(
                        "Cross-source public funding intelligence for Jacksonville / Duval County. "
                        + "Every monetary value returned by this API is accompanied by an "
                        + "amountClass describing what it measures (appropriated, programmed, "
                        + "awarded, obligated, outlayed). Figures of different classes are not "
                        + "comparable and must not be summed.")
                .license(new License().name("MIT")));
    }
}
