package com.ureca.only4_be.batch.jobs.notification.step;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.batch.jobs.notification.processor.BillToNotificationProcessor;
import com.ureca.only4_be.batch.jobs.notification.writer.BillStatusUpdateWriter;
import com.ureca.only4_be.batch.jobs.notification.writer.NotificationKafkaWriter;
import com.ureca.only4_be.domain.bill.Bill;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class NotificationStepConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JpaPagingItemReader<Bill> notificationReader;
    private final BillToNotificationProcessor notificationProcessor;
    private final NotificationKafkaWriter notificationKafkaWriter;
    private final BillStatusUpdateWriter billStatusUpdateWriter;

    @Bean
    public Step notificationStep() {
        return new StepBuilder("notificationStep", jobRepository)
                .<Bill, NotificationRequest>chunk(100, transactionManager)
                .reader(notificationReader)
                .processor(notificationProcessor)
                .writer(compositeWriter())
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
