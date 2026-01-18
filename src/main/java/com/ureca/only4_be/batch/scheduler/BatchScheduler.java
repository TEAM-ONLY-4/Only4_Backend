package com.ureca.only4_be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    // 테스트용: 10초마다 실행 (로그 확인 후 주석 처리)
    // @Scheduled(cron = "0/10 * * * * *")
    // 실제 운영용: 매월 1일 새벽 4시 -> @Scheduled(cron = "0 0 4 1 * *")
    public void runMonthlySettlement() {
        try {
            log.info("[Scheduler] 정산 배치 자동 실행 시작");
            Job job = jobRegistry.getJob("settlementJob");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", LocalDateTime.now().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);
        } catch (Exception e) {
            log.error("[Scheduler] 배치 실행 중 에러 발생", e);
        }
    }
}