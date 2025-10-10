package com.moden.modenapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for the API.
 * <p>
 * Enables cross-origin requests for Swagger UI, local development,
 * and production deployments (e.g., Render or frontend domains).
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // Local Swagger UI or frontend access
                        .allowedOrigins(
                                "http://localhost:8080",
                                "http://localhost:5173",
                                "https://moden-back.onrender.com",
                                "https://moden-web.vercel.app" // example frontend domain
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
