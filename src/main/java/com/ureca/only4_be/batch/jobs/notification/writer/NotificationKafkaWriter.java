package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.kafka.service.EmailKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaWriter implements ItemWriter<NotificationRequest> {

    private final EmailKafkaProducer emailKafkaProducer;

    @Override
    public void write(Chunk<? extends NotificationRequest> chunk) {
        // Chunk 단위로 들어온 데이터를 반복문으로 전송
        for (NotificationRequest request : chunk) {
            emailKafkaProducer.send(request);
        }

        // 2. 로그 기록 (어떤 ID들이 전송되었는지 명시)
        if (log.isInfoEnabled()) {
            List<Long> ids = chunk.getItems().stream()
                    .map(NotificationRequest::getNotificationId) // notificationId 로깅
                    .collect(Collectors.toList());

            log.info(">>> 🚀 [KafkaWriter] Kafka 전송 완료 ({}건): IDs={}", chunk.size(), ids);
        }
    }
}
