package com.ureca.only4_be.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    // 테스트용 URL: POST http://localhost:8080/api/batch/settlement
    @GetMapping("/settlement")
    public String runSettlementJob(@RequestParam(value = "date", required = false) String date) {
        try {
            // "settlementJob" 이라는 이름의 Bean을 찾아서 실행
            Job job = jobRegistry.getJob("settlementJob");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("date", date != null ? date : LocalDateTime.now().toString())
                    .addLong("time", System.currentTimeMillis()) // 매번 새로운 Job으로 인식시키기 위함
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);

            return "배치 실행 성공! (로그를 확인하세요)";
        } catch (Exception e) {
            e.printStackTrace();
            return "배치 실행 실패: " + e.getMessage();
        }
    }
}