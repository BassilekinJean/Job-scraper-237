package com.cameroun.jobscraper.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class Resilience4jConfig {

    private final RateLimiterRegistry rateLimiterRegistry;

    public Resilience4jConfig(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Bean
    public RateLimiter loginRateLimiter() {
        return rateLimiterRegistry.rateLimiter("loginRateLimiter");
    }

    @Bean
    public RateLimiter registerRateLimiter() {
        return rateLimiterRegistry.rateLimiter("registerRateLimiter");
    }
}