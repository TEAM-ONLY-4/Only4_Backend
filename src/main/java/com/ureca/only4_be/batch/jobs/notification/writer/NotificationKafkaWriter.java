package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.kafka.service.EmailKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

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

        log.info(">>> 🚀 [KafkaWriter] {} 건의 청구서 메시지를 Kafka로 전송했습니다.", chunk.size());
    }
}
