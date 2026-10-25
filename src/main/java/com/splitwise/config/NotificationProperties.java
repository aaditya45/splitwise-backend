package com.splitwise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("http://localhost:8090") String baseUrl,
        @DefaultValue("dev-internal-key-change-me") String internalApiKey,
        @DefaultValue("2000") int connectTimeoutMs,
        @DefaultValue("5000") int readTimeoutMs) {
}