package com.ureca.only4_be.kafka.service;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
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

    public void send(NotificationRequest notificationRequest) {
        String topic = kafkaTopicsProperties.emailRequest();

        kafkaTemplate.send(topic, String.valueOf(notificationRequest.getBillId()), notificationRequest)
                .whenComplete((result, ex) -> {
                    if(ex != null) {
                        log.error("[Email Producer] 발행 실패. topic={}, memberId={}, billId={}, error={}",
                                topic, notificationRequest.getMemberId(), notificationRequest.getBillId(), ex.getMessage());
                    } else {
                        log.info("[Email Producer] 발행 성공. topic={}, memberId={}, billId={}",
                                topic, notificationRequest.getMemberId(), notificationRequest.getBillId());
                    }
                });
        }
}
