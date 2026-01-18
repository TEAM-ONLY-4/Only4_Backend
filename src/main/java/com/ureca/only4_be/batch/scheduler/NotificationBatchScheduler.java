package com.ureca.only4_be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
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

    // 10시~20시 사이 2시간 간격 실행 (10, 12, 14, 16, 18, 20시)
    // 21시 이후(밤)와 08시 이전(아침)에는 실행되지 않음
    @Scheduled(cron = "0 0 10-20/2 * * *")
    public void runNotificationJob() {
        log.info(">>> [Scheduler] 청구서 발송 배치 시작! 시간: {}", LocalDateTime.now());

        try {
            String dateParam = LocalDate.now().toString();

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", dateParam)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(notificationJob, jobParameters);

        } catch (Exception e) {
            log.error(">>> [Scheduler] 배치 실행 중 에러 발생", e);
        }
    }
}