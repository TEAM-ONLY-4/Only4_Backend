package com.ureca.only4_be.batch.jobs.settlement.step;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SettlementSourceDto;
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

@Component
@RequiredArgsConstructor
public class SettlementStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final SettlementItemReader reader;
    private final MonthlySettlementProcessor processor;
    private final SettlementItemWriter writer;

    @Bean
    public Step settlementStep() {
        return new StepBuilder("settlementStep", jobRepository)
                .<SettlementSourceDto, BillResultDto>chunk(1000, transactionManager) // 트랜잭션 단위 100개
                .reader(reader)
                .stream(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}