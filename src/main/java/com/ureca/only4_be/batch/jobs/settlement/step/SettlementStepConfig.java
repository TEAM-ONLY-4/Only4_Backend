package com.ureca.only4_be.batch.jobs.settlement.step;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SettlementSourceDto;
import com.ureca.only4_be.batch.jobs.settlement.listener.SettlementSkipListener;
import com.ureca.only4_be.batch.jobs.settlement.processor.MonthlySettlementProcessor;
import com.ureca.only4_be.batch.jobs.settlement.reader.SettlementItemReader;
import com.ureca.only4_be.batch.jobs.settlement.writer.SettlementItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class SettlementStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final SettlementItemReader reader;
    private final MonthlySettlementProcessor processor;
    private final SettlementItemWriter writer;

    private final SettlementSkipListener settlementSkipListener;

    @Bean
    public Step settlementStep() {

        // [정책 생성] 1000ms(1초) 대기 정책
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L); // 1초 (단위: ms)

        return new StepBuilder("settlementStep", jobRepository)
                .<SettlementSourceDto, BillResultDto>chunk(1000, transactionManager) // 트랜잭션 단위 100개
                .reader(reader)
                .processor(processor)
                .writer(writer)

                // ===========================================
                // 방어 모드 시작 (Fault Tolerant)
                // ===========================================
                .faultTolerant()

                // 1. [Retry] 재시도 설정 (네트워크, DB 락 등 일시적 장애)
                .retry(SQLException.class)            // DB 에러
                .retry(java.net.ConnectException.class) // 연결 에러
                .retryLimit(3)                        // 3번까지 다시 시도해봄
                .backOffPolicy(backOffPolicy)

                // 2. [Skip] 건너뛰기 설정 (데이터 오류, 로직 오류)
                .skip(NullPointerException.class)     // 데이터 누락
                .skip(IllegalArgumentException.class) // 잘못된 값
                .skip(ArithmeticException.class)      // 0으로 나누기 등
                .skipLimit(100)                       // 100개까지는 허용, 그 이상은 배치 중단

                // 3. [Listener] 스킵된 것들 기록하기
                .listener(settlementSkipListener)

                .build();
    }
}