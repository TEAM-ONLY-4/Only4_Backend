package com.ureca.only4_be.batch.jobs.notification.reader;

import com.ureca.only4_be.domain.bill.Bill;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class StagingItemReader {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaCursorItemReader<Bill> notificationStagingReader(
            // 배치 실행 시 파라미터로 '청구년월'을 받기
            @Value("#{jobParameters['billingDate']}") String billingDateStr
    ){
        // ★ [핵심 수정] 파라미터로 뭐가 들어오든 무조건 '1일'로 강제 변환
        LocalDate targetBillingMonth = (billingDateStr != null)
                ? LocalDate.parse(billingDateStr).withDayOfMonth(1) // "2026-05-21" -> "2026-05-01"
                : LocalDate.now().withDayOfMonth(1);                // 오늘이 21일이라도 "1일"로 설정

        int todayDay = LocalDate.now().getDayOfMonth();
        LocalTime nowTime = LocalTime.now();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetBillingMonth", targetBillingMonth);
        parameters.put("today", (short) todayDay);
        parameters.put("currentTime", nowTime);

        return new JpaCursorItemReaderBuilder<Bill>()
                .name("notificationStatingReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT b FROM Bill b " +
                                "JOIN FETCH b.member m " +
                                "LEFT JOIN BillNotification bn ON bn.bill = b " +
                                "WHERE b.billingYearMonth = :targetBillingMonth " +
                                "AND bn.id IS NULL " +
                                "AND (m.notificationDayOfMonth IS NULL OR m.notificationDayOfMonth <= :today) " +
                                "AND (" +
                                "   (m.doNotDisturbStartTime IS NULL OR m.doNotDisturbEndTime IS NULL) " +
                                "   OR " +
                                "   (m.doNotDisturbStartTime < m.doNotDisturbEndTime " +
                                "       AND (:currentTime < m.doNotDisturbStartTime OR :currentTime > m.doNotDisturbEndTime)) " +
                                "   OR " +
                                "   (m.doNotDisturbStartTime > m.doNotDisturbEndTime " +
                                "       AND (:currentTime > m.doNotDisturbEndTime AND :currentTime < m.doNotDisturbStartTime)) " +
                                ") " +
                                "ORDER BY b.id ASC"
                )
                .parameterValues(parameters)
                .build();
    }
}
