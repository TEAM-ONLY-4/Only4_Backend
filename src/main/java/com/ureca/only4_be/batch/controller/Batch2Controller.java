package com.ureca.only4_be.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/batch/notifications")
@RequiredArgsConstructor
public class Batch2Controller {

    private final JobLauncher jobLauncher;

    @Qualifier("notificationStagingJob")
    private final Job notificationStagingJob;

    @Qualifier("notificationPublishingJob")
    private final Job notificationPublishingJob;

    @GetMapping("/staging")
    public String runStagingJob(
            @RequestParam(value = "date", required = false) String requestDate
    ) {
        if (requestDate == null) {
            requestDate = LocalDate.now().toString(); // 기본값: 오늘
        }
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("billingDate", requestDate)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(notificationStagingJob, jobParameters);

            return "[Step 1] 적재 배치 실행 완료! (청구 기준일: " + requestDate + ")";
        } catch (Exception e) {
            e.printStackTrace();
            return "[Step 1] 실행 실패: " + e.getMessage();
        }
    }

    @GetMapping("/publish")
    public String runPublishingJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(notificationPublishingJob, jobParameters);
            return "🚀 [Step 2] 전송(Publishing) 배치 실행 완료!";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ [Step 2] 실행 실패: " + e.getMessage();
        }
    }
}