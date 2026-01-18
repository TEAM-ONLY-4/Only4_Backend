package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaWriter implements ItemWriter<NotificationRequest> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 토픽 이름 yml 주입
    @Value("${spring.kafka.template.notification-topic}")
    private String topicName;

    @Override
    public void write(Chunk<? extends NotificationRequest> chunk) {
        // Chunk 단위로 들어온 데이터를 반복문으로 전송
        for (NotificationRequest request : chunk) {

            // 메시지 키(Key) 생성: 같은 ID를 가진 메시지는 무조건 같은 파티션
            String messageKey = String.valueOf(request.getBillId());

            // 전송 (비동기)
            // 파라미터: (토픽이름, 파티션키, 데이터)
            CompletableFuture<?> future = kafkaTemplate.send(topicName, messageKey, request);

        }

        log.info(">>> 🚀 [KafkaWriter] {} 건의 청구서 메시지를 Kafka로 전송했습니다.", chunk.size());
    }
}
