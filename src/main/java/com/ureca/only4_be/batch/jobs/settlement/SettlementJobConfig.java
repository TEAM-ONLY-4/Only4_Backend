package com.ureca.only4_be.batch.jobs.settlement;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SettlementSourceDto;
import com.ureca.only4_be.batch.jobs.settlement.partitioner.SettlementPartitioner;
import com.ureca.only4_be.batch.jobs.settlement.processor.MonthlySettlementProcessor;
import com.ureca.only4_be.batch.jobs.settlement.reader.SettlementItemReader;
import com.ureca.only4_be.batch.jobs.settlement.step.SettlementStepConfig;
import com.ureca.only4_be.batch.jobs.settlement.writer.SettlementItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 구현한 컴포넌트들
    private final SettlementPartitioner partitioner;
    private final SettlementItemReader reader;
    private final MonthlySettlementProcessor processor;
    private final SettlementItemWriter writer; // JDBC Writer

    // =========================================
    // 1. Thread Pool (알바생 대기소)
    // =========================================
    @Bean
    public TaskExecutor settlementTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // CorePoolSize = 스레드 수
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("settlement-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    // =========================================
    // 2. Job 정의
    // =========================================
    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementMasterStep()) // Master Step 시작
                .build();
    }

    // =========================================
    // 3. Master Step (관리자)
    // =========================================
    @Bean
    public Step settlementMasterStep() {
        return new StepBuilder("settlementMasterStep", jobRepository)
                .partitioner("settlementSlaveStep", partitioner) // 파티셔너 등록
                .step(settlementSlaveStep()) // 일꾼 등록
                .gridSize(5) // (= 파티션 수 != 스레드 수)
                .taskExecutor(settlementTaskExecutor()) // 병렬 실행기
                .build();
    }

    // =========================================
    // 4. Slave Step (실제 일꾼)
    // =========================================
    @Bean
    public Step settlementSlaveStep() {
        return new StepBuilder("settlementSlaveStep", jobRepository)
                .<SettlementSourceDto, BillResultDto>chunk(1000, transactionManager) // Chunk Size 1000
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}