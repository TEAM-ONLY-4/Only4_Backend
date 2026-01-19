package com.ureca.only4_be.kafka.service;

import com.ureca.only4_be.kafka.event.EmailSendRequestEvent;
import com.ureca.only4_be.kafka.properties.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public void send(EmailSendRequestEvent emailSendRequestEvent) {
        String topic = kafkaTopicsProperties.emailRequest();

        kafkaTemplate.send(topic, emailSendRequestEvent)
                .whenComplete((result, ex) -> {
                    if(ex != null) {
                        log.error("[Email Producer] 발행 실패. topic={}, memberId={}, billId={}, error={}",
                                topic, emailSendRequestEvent.memberId(), emailSendRequestEvent.billId(), ex.getMessage());
                    } else {
                        log.info("[Email Producer] 발행 성공. topic={}, memberId={}, billId={}",
                                topic, emailSendRequestEvent.memberId(), emailSendRequestEvent.billId());
                    }
                });
        }
}
