package com.ureca.only4_be.batch.jobs.notification.reader;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillSendStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class NotificationItemReader {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaPagingItemReader<Bill> notificationReader(
            @Value("#{jobParameters['requestDate']}") String requestDateStr){

        LocalDate targetDate = (requestDateStr != null)
                ? LocalDate.parse(requestDateStr)
                : LocalDate.now();

        int targetDay = targetDate.getDayOfMonth(); //일 -> 유저의 설정날짜와 비교
        LocalTime nowTime = LocalTime.now(); // 시 -> 방해금지 시간 비교
        LocalDate currentBillingMonth = targetDate.withDayOfMonth(1);//이번 달 청구서만 조회하기 위한 기준

        Map<String, Object> parameters = new HashMap<>(); //쿼리문에 들어갈 변수 저장 위치(파라미터로 넘기는 게 보안상 안전)
        parameters.put("status1", BillSendStatus.BEFORE_SENT);
        parameters.put("status2", BillSendStatus.FAILED);

        parameters.put("notificationDayOfMonth", (short) targetDay);
        parameters.put("currentTime", nowTime);
        parameters.put("billingMonth", currentBillingMonth);

        return new JpaPagingItemReaderBuilder<Bill>() //bill객체 읽어오는 reader
                .name("notificationReader") //Execution Context
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString(
                        "SELECT b FROM Bill b " +
                        "JOIN FETCH b.member m " + // N+1 방지
                        "WHERE (b.billSendStatus = :status1 OR b.billSendStatus = :status2) " +
                        "AND m.notificationDayOfMonth <= :notificationDayOfMonth " +// 지난 날짜들 까지 조회 --- 실패처리
                        "AND b.billingYearMonth = :billingMonth " +
                        "AND (" +
                        //  금지 시간을 설정하지 않은 사람은 무조건 통과 (NULL 체크)
                        "   (m.doNotDisturbStartTime IS NULL OR m.doNotDisturbEndTime IS NULL) " +
                        "   OR " +
                        // 일반 케이스 (예: 13:00 ~ 15:00 금지) -> 13시 전이거나 15시 후여야 함
                        "   (m.doNotDisturbStartTime < m.doNotDisturbEndTime " +
                        "       AND (:currentTime < m.doNotDisturbStartTime OR :currentTime > m.doNotDisturbEndTime)) " +
                        "   OR " +
                        // 밤샘 케이스 (예: 23:00 ~ 07:00 금지) -> 07시~23시 사이여야 함
                        "   (m.doNotDisturbStartTime > m.doNotDisturbEndTime " +
                        "       AND (:currentTime > m.doNotDisturbEndTime AND :currentTime < m.doNotDisturbStartTime)) " +
                        ") " +
                        "ORDER BY m.notificationDayOfMonth ASC, b.id ASC" // (5) PagingReader는 정렬 필수
                )
                .parameterValues(parameters)
                .build();
    }
}
