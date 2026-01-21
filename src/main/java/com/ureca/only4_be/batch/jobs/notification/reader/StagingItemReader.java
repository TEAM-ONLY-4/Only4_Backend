package com.ureca.only4_be.batch.jobs.notification.reader;

import com.ureca.only4_be.domain.bill.Bill;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
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
    public JpaPagingItemReader<Bill> notificationDataStagingReader(
            // 배치 실행 시 파라미터로 '청구년월'을 받기
            @Value("#{jobParameters['billingDate']}") String billingDateStr
    ){
        int todayDay = LocalDate.now().getDayOfMonth();
        LocalTime nowTime = LocalTime.now();

        // 청구년월 기준일 (파라미터 없으면 이번 달 1일로 가정)
        LocalDate targetBillingMonth = (billingDateStr !=null)
                ? LocalDate.parse(billingDateStr)
                : LocalDate.now().withDayOfMonth(1);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tody", (short) todayDay);
        parameters.put("currentTime", nowTime);
        parameters.put("targerBillingMonth", targetBillingMonth);

        return new JpaPagingItemReaderBuilder<Bill>()
                .name("notificationStatingReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString(
                        "SELECT b FROM Bill b" +
                        "JOIN FETCH b.member m "+
                        "LEFT JOIN BillNotification bn ON bn.bill = b" + //객체연관관계 조회
                        "WHERE b.billingYearMonth = :targetBillingMonth" + //이번 달 청구서 중에서
                        "AND bn.id IS NULL"+ // 1. 알림 테이블에 아예 없는 것만 가져오기
                        "AND m.notificationDayOfMonth <= :today" +
                        "AND (" +
                        "   (m.doNotDisturbStartTime IS NULL OR m.doNotDisturbEndTime IS NULL) " +
                        "   OR " +
                        "   (m.doNotDisturbStartTime < m.doNotDisturbEndTime " +
                        "       AND (:currentTime < m.doNotDisturbStartTime OR :currentTime > m.doNotDisturbEndTime)) " +
                        "   OR " +
                        "   (m.doNotDisturbStartTime > m.doNotDisturbEndTime " +
                        "       AND (:currentTime > m.doNotDisturbEndTime AND :currentTime < m.doNotDisturbStartTime)) " +
                        ")"
                )
                .parameterValues(parameters)
                .build();
    }
}
