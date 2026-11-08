package com.secondstitch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Decides which websites may change data through this API (CORS).
 * Vite moves to 5174, 5175... if 5173 is busy, so any localhost port is allowed.
 * When deploying, set CORS_ORIGINS=https://your-frontend.vercel.app
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    public CorsConfig(@Value("${app.cors-origins:http://localhost:*,http://127.0.0.1:*}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}