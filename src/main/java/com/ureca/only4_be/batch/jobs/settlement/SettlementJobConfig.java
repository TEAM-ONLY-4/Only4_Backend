package com.ureca.only4_be.batch.jobs.settlement;

import com.ureca.only4_be.batch.jobs.settlement.step.SettlementStepConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final SettlementStepConfig settlementStepConfig;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStepConfig.settlementStep()) // Step 실행
                .build();
    }
}