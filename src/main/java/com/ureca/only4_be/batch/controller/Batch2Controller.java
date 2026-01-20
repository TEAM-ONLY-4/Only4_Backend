package com.ureca.only4_be.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class Batch2Controller {

    private final JobLauncher jobLauncher;

    @Qualifier("notificationJob") // 설정 파일에서 만든 Job Bean 이름
    private final Job notificationJob;

    @GetMapping("/batch/notification")
    public String runNotificationJob(
            @RequestParam(value = "date", required = false) String requestDate
    ) {
        // 날짜 파라미터가 없으면 오늘 날짜로 설정
        if (requestDate == null) {
            requestDate = LocalDate.now().toString();
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", requestDate)
                    .addLong("time", System.currentTimeMillis()) // 중복 실행 방지용
                    .toJobParameters();

            jobLauncher.run(notificationJob, jobParameters);

            return "✅ 배치 실행 완료! (기준일: " + requestDate + ")";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 배치 실행 실패: " + e.getMessage();
        }
    }
}