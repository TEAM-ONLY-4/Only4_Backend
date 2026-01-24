package com.ureca.only4_be.kafka.config;

import com.ureca.only4_be.kafka.properties.KafkaTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private final KafkaTopicsProperties properties;

    public KafkaTopicConfig(KafkaTopicsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NewTopic emailRequestTopic() {
        return TopicBuilder.name(properties.emailRequest())
                .partitions(properties.partitions())
                .replicas(1)
                .build();
    }
}
