package com.ureca.only4_be.batch.jobs.notification.reader;

import com.ureca.only4_be.domain.bill_notification.BillNotification;
import com.ureca.only4_be.domain.bill_notification.PublishStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class NotificationItemReader {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaCursorItemReader<BillNotification> notificationReader(){

        Map<String, Object> parameters = new HashMap<>(); //쿼리문에 들어갈 변수 저장 위치(파라미터로 넘기는 게 보안상 안전)
        parameters.put("pending", PublishStatus.PENDING); // 처음 보내는 대기 상태
        parameters.put("failed", PublishStatus.FAILED);   // 이전에 실패해서 재시도해야 할 상태

        return new JpaCursorItemReaderBuilder<BillNotification>() //bill객체 읽어오는 reader
                .name("notificationReader") //Execution Context
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT bn FROM BillNotification bn " +
                        "JOIN FETCH bn.bill b " +
                        "JOIN FETCH b.member m " +
                        "WHERE bn.publishStatus IN (:pending, :failed) " +
                        "ORDER BY bn.id ASC"
                )
                .parameterValues(parameters)
                .build();
    }
}
