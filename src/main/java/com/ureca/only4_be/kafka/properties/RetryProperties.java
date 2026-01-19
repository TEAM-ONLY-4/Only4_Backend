package com.ureca.only4_be.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.retry")
public record RetryProperties (
        int emailMaxAttempts,
        long initialIntervalMs,
        long maxIntervalMs
) {}
