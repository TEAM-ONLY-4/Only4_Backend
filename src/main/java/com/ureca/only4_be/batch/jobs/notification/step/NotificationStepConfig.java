package com.ureca.only4_be.batch.jobs.notification.step;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.batch.jobs.notification.processor.BillToNotificationProcessor;
import com.ureca.only4_be.domain.bill.Bill;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
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

    @Bean
    public Step notificationStep() {
        return new StepBuilder("notificationStep", jobRepository)
                .<Bill, NotificationRequest>chunk(1000, transactionManager)
                .reader(notificationReader)
                .processor(notificationProcessor)
                .writer(tempConsoleWriter()) // 임시 Writer
                .build();
    }

    // 임시 Writer (콘솔 출력용)
    // Kafka 연결 전에 데이터가 여기까지 잘 넘어오는지 눈으로 확인하는 용도
    @Bean
    public ItemWriter<NotificationRequest> tempConsoleWriter() {
        return items -> {
            System.out.println("===== [Writer] 쓰기 작업 시작 (Chunk Size: " + items.size() + ") =====");
            for (NotificationRequest item : items) {
                System.out.println(">>> 전송할 알림 데이터: " + item);
            }
            System.out.println("==========================================================");
        };
    }
}
