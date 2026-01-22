package com.ureca.only4_be.batch.jobs.notification.step;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.batch.jobs.notification.listener.NotificationSkipListener;
import com.ureca.only4_be.batch.jobs.notification.listener.NotificationStep2SkipListener;
import com.ureca.only4_be.batch.jobs.notification.processor.NotificationPublishProcessor;
import com.ureca.only4_be.batch.jobs.notification.writer.NotificationStatusUpdateWriter;
import com.ureca.only4_be.batch.jobs.notification.writer.NotificationKafkaWriter;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill_notification.BillNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.TransientDataAccessException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Configuration
@RequiredArgsConstructor
public class NotificationStep2Config {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ItemReader<BillNotification> notificationReader;
    private final NotificationPublishProcessor notificationProcessor;
    private final NotificationKafkaWriter notificationKafkaWriter;
    private final NotificationStatusUpdateWriter billStatusUpdateWriter;
    private final NotificationStep2SkipListener notificationStep2SkipListener;

    @Bean
    public Step step2Publishing() {
        return new StepBuilder("step2Publishing", jobRepository)
                .<BillNotification, NotificationRequest>chunk(1000, transactionManager)
                .reader(notificationReader)
                .processor(notificationProcessor)
                .writer(compositeWriter())
                //----실패 처리 전략 ----
                .faultTolerant()// 내결함성(Fault Tolerance) 활성화

                // Retry 설정 (네트워크 문제) 청크 재시도
                .retry(TimeoutException.class)       // Kafka 타임아웃
                .retry(ConnectException.class)       // 연결 실패
                .retry(org.apache.kafka.common.errors.NetworkException.class) // Kafka 네트워크 에러
                .retry(org.apache.kafka.common.errors.RetriableException.class) // Kafka 재시도 가능 에러
                .retry(TransientDataAccessException.class) // DB 일시적 장애
                .retryLimit(3)                       // 에러 발생 시 3번까지 재시도

                // 1. Skip 설정 (데이터 문제) 하나만 스킵
                .skip(IllegalArgumentException.class)
                .skipLimit(100)             // 허용 가능한 에러 개수 (100개 넘으면 배치 실패)

                // 3. Listener 등록
                .listener(notificationStep2SkipListener) // Skip 발생 시 로그 남기기
                .build();
    }

    // 두 Writer를 하나로 묶어주는 역할 (순서: Kafka 전송 -> DB 업데이트)
    @Bean
    public CompositeItemWriter<NotificationRequest> compositeWriter() {
        return new CompositeItemWriterBuilder<NotificationRequest>()
                .delegates(notificationKafkaWriter, billStatusUpdateWriter)
                .build();
    }
}
