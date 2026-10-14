package com.shopsphere.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shopSphereOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere REST API")
                        .description("Full-Stack E-commerce & Inventory Management Platform API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ShopSphere Engineering Team")
                                .email("dev@shopsphere.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
