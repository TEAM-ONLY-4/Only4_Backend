package com.ureca.only4_be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
public class NotificationBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job stagingJob;
    private final Job publishingJob;

    public NotificationBatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("notificationStagingJob") Job stagingJob,
            @Qualifier("notificationPublishingJob") Job publishingJob
    ) {
        this.jobLauncher = jobLauncher;
        this.stagingJob = stagingJob;
        this.publishingJob = publishingJob;
    }

    // 10시~20시 사이 2시간 간격 실행 (10, 12, 14, 16, 18, 20시)
    // 21시 이후(밤)와 08시 이전(아침)에는 실행되지 않음
    @Scheduled(cron = "0 0 10-20/2 * * *")
    public void runNotificationJob() {
        log.info(">>> [Scheduler] 청구서 발송 배치 시작! 시간: {}", LocalDateTime.now());

        String todayDate = LocalDate.now().toString();
        try {
            log.info(">>> [Step 1] 적재 Job 시작");

            JobParameters stagingParams = new JobParametersBuilder()
                    .addString("billingDate", todayDate)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(stagingJob, stagingParams);
            log.info(">>> [Step 1] 적재 Job 성공!");

        } catch (Exception e) {
            log.error(">>> [Step 1] 적재 Job 실패. 배치를 중단합니다.", e);
            return;
        }
        // 2. 전송(Publishing) Job 실행 (Step 1 성공 시에만 실행됨)
        try {
            log.info(">>> [Step 2] 전송 Job 시작");
            JobParameters publishingParams = new JobParametersBuilder()
                    .addString("billingDate", todayDate) // 로깅용으로 남겨둠
                    .addLong("time", System.currentTimeMillis()+ 1) // timestamp 다르게 찍어서 별도 실행 취급
                    .toJobParameters();

            jobLauncher.run(publishingJob, publishingParams);
            log.info(">>> [Step 2] 전송 Job 성공!");

        } catch (Exception e) {
            log.error(">>> [Step 2] 전송 Job 실패", e.getMessage());
        }
    }
}