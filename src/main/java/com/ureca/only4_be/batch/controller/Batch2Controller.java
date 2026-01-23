package com.ureca.only4_be.batch.controller;

import com.ureca.only4_be.batch.controller.BatchJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class Batch2Controller {

    private final JobLauncher jobLauncher;

    @Qualifier("notificationJob")
    private final Job notificationJob;

    @GetMapping("/notifications")
    public ResponseEntity<?> runNotificationBatch(
            @RequestParam(value = "date", required = false) String requestDate
    ) {
        if (requestDate == null) {
            requestDate = LocalDate.now().toString();
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("billingDate", requestDate)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // JobExecution 객체를 반환받음
            JobExecution execution = jobLauncher.run(notificationJob, jobParameters);

            // DTO로 변환하여 반환
            return ResponseEntity.ok(BatchJobResponse.from(execution));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("배치 실행 실패: " + e.getMessage());
        }
    }
}