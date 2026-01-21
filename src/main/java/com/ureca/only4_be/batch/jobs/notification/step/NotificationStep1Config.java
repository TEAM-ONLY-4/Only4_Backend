package com.ureca.only4_be.batch.jobs.notification.step;

import com.ureca.only4_be.batch.jobs.notification.processor.StagingProcessor;
import com.ureca.only4_be.batch.jobs.notification.writer.StagingWriter;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill_notification.BillNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class NotificationStep1Config {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final JpaPagingItemReader<Bill> notificationStagingReader;
    private final StagingProcessor stagingProcessor;
    private final StagingWriter stagingWriter;

    @Bean
    public Step step1Staging(){
        return new StepBuilder("step1Staging", jobRepository)
                .<Bill, BillNotification>chunk(1000, transactionManager)
                .reader(notificationStagingReader)
                .processor(stagingProcessor)
                .writer(stagingWriter)
                .build();
    }
}
