package com.secondstitch.config;

import com.secondstitch.auth.OwnerPasswordGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Runs OwnerPasswordGuard before every API request. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OwnerPasswordGuard ownerPasswordGuard;

    public WebConfig(OwnerPasswordGuard ownerPasswordGuard) {
        this.ownerPasswordGuard = ownerPasswordGuard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ownerPasswordGuard).addPathPatterns("/api/**");
    }
}
