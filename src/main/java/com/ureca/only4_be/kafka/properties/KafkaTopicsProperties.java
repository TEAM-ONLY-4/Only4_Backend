package com.ureca.only4_be.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String groupId,
        String emailRequest,
        Integer partitions
) {}
