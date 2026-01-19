package com.ureca.only4_be.kafka.config;

import com.ureca.only4_be.kafka.properties.KafkaTopicsProperties;
import com.ureca.only4_be.kafka.properties.RetryProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
@EnableConfigurationProperties({RetryProperties.class, KafkaTopicsProperties.class}) // 이거 추가 필요!
@Configuration
public class KafkaProducerConfig {
    private final RetryProperties retryProperties;

    public KafkaProducerConfig(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
        // 직렬화 설정
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 멱등성 프로듀서 설정 (중복 없는 전송)
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카 승인 대기
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // 메시지 순서 보장 성능 최적화
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 재시도 횟수 (멱등성 보장 위해 무한대)
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryProperties.initialIntervalMs()); // 재시도 사이의 대기 시간
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, (int) retryProperties.maxIntervalMs()); // 전체 타임아웃 시간

        // 서로 다른 프로젝트 간 JSON 통신 시 패키지 에러 방지
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}