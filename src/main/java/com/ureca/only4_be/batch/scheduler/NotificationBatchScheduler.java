package com.ureca.only4_be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class NotificationBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job notificationJob;

    // 10시~20시 2시간 간격
    // @Scheduled(cron = "0 0 10-20/2 * * *")
    public void runNotificationJob() {
        log.info(">>> [Scheduler] 청구서 발송 통합 배치 시작! 시간: {}", LocalDateTime.now());
        String todayDate = LocalDate.now().toString();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("billingDate", todayDate)
                    .addLong("time", System.currentTimeMillis()) // 중복 실행 방지용 ID
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(notificationJob, jobParameters);

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info(">>> ✅ [Scheduler] 통합 배치 성공적으로 완료!");
            } else {
                log.error(">>> 🚨 [Scheduler] 배치 실패 (Step 1 또는 Step 2 중 실패). Status: {}", execution.getStatus());
            }

        } catch (Exception e) {
            log.error(">>> 🚨 [Scheduler] 배치 실행 중 심각한 오류 발생", e);
        }
    }
}