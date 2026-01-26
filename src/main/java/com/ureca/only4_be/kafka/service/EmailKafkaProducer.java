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

        log.info("[Email Producer] 전송 시도 시작: topic={}, memberId={}, billId={}",
                topic, notificationRequest.getMemberId(), notificationRequest.getBillId());

        try {
            kafkaTemplate.send(topic, String.valueOf(notificationRequest.getBillId()), notificationRequest)
                    .whenComplete((result, ex) -> {
                        if(ex != null) {
                            log.error("[Email Producer] 콜백 응답 - 발행 실패. billId={}, error={}",
                                    notificationRequest.getBillId(), ex.getMessage());
                        } else {
                            log.info("[Email Producer] 콜백 응답 - 발행 성공. billId={}, offset={}",
                                    notificationRequest.getBillId(), result.getRecordMetadata().offset());
                        }
                    });
            log.info("[Email Producer] kafkaTemplate.send() 호출 완료 (비동기 대기 중). billId={}", notificationRequest.getBillId());
        } catch (Exception e) {
            log.error("[Email Producer] 호출 중 즉각 예외 발생. billId={}, error={}",
                    notificationRequest.getBillId(), e.getMessage(), e);
        }
    }
}
