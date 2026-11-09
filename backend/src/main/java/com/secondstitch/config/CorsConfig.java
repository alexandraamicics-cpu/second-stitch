package com.secondstitch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Decides which websites may change data through this API (CORS).
 *
 * Locally, Vite forwards /api requests to port 8080, and the browser's
 * "Origin" is wherever the frontend runs. That's usually localhost:5173, but
 * Vite moves to 5174, 5175... if 5173 is busy, so any localhost port is allowed.
 * Without this, adding or removing data fails with "403 Invalid CORS request".
 *
 * When deploying, set CORS_ORIGINS=https://your-frontend.vercel.app
 * (see application-postgres.properties).
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
                // "Patterns" allow wildcards like http://localhost:*, and exact addresses too
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
