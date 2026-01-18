package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@EmbeddedKafka(partitions = 1, topics = {"billing-notification-topic"})
class NotificationKafkaWriterTest {

    @Autowired
    private NotificationKafkaWriter kafkaWriter;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Writer가 Kafka 토픽으로 메시지를 정상 전송하는지 검증")
    void testWrite() {
        // [1. Consumer Setup]
        String groupName = "testGroup-" + UUID.randomUUID().toString();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupName, "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();

        // Consumer는 'billing-notification-topic'을 구독함
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "billing-notification-topic");

        // [2. Producer Data Setup]
        NotificationRequest request = NotificationRequest.builder()
                .billId(123L)
                .memberId(1L)
                .totalAmount(BigDecimal.valueOf(10000))
                .build();

        Chunk<NotificationRequest> chunk = new Chunk<>(List.of(request));

        // [3. Action]
        kafkaWriter.write(chunk);

        // [4. Assertion]
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "billing-notification-topic", Duration.ofSeconds(10));

        assertThat(record.key()).isEqualTo("123");
        assertThat(record.value()).contains("10000");

        System.out.println(">>> ✅ 테스트 성공: 수신된 메시지 = " + record.value());
    }
}