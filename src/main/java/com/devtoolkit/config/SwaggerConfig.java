package com.devtoolkit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port}")
    private String port;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API REST DevToolkit AI")
                        .description("API REST utilitaires en ligne destine aux developpeurs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe  de développement")
                                .email("loubahasra.dev@gmail.com")
                                .url("https://loubahasra-dev.netlify.app"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
//                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
//                .components(new Components()
//                        .addSecuritySchemes("bearerAuth",
//                                new SecurityScheme()
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer").bearerFormat("JWT")
//                                        .description("Token JWT pour l'authentification")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:"+port)
                                .description("Serveur de développement"),
                        new Server()
                        .url("http://backend-api.app")
                        .description("Serveur de production")
                ));
    }
}
