package com.vertexplanner.vertexai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {


    String yourLocalIp = "192.168.100.67";
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Allow all API paths
                .allowedOrigins(
                        "http://localhost:3000", // Default React port
                        "http://localhost:5173", // Default Vite port
                        "http://%s:3000".formatted(yourLocalIp), // For your roommate
                        "http://%s:5173".formatted(yourLocalIp)  // For your roommate
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
