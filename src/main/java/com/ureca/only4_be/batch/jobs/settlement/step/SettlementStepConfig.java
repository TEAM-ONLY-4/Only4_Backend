package com.ureca.only4_be.batch.jobs.settlement.step;

import com.ureca.only4_be.batch.jobs.settlement.processor.MonthlySettlementProcessor;
import com.ureca.only4_be.batch.jobs.settlement.reader.SettlementItemReader;
import com.ureca.only4_be.batch.jobs.settlement.writer.SettlementItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 아까 만든 3형제 주입
    private final SettlementItemReader reader;
    private final MonthlySettlementProcessor processor;
    private final SettlementItemWriter writer;

    @Bean
    public Step settlementStep() {
        return new StepBuilder("settlementStep", jobRepository)
                .<String, String>chunk(10, transactionManager) // [중요] 제네릭 타입 <String, String> 확인
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}