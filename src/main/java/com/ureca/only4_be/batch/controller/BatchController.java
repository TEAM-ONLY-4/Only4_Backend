package com.ureca.only4_be.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    // 호출 예시: POST /api/batch/settlement?date=2026-01-05
    @GetMapping("/settlement") // 테스트용
    // @PostMapping("/settlement")
    public String runSettlementJob(@RequestParam(value = "date", required = false) String date) {
        try {
            String targetDate = (date != null) ? date : LocalDate.now().toString();
            log.info("[API Trigger] 정산 배치 실행 시작! (Target: {})", targetDate); // 시작 로그

            Job job = jobRegistry.getJob("settlementJob");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", targetDate)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // 실행 결과(JobExecution)를 받음
            JobExecution execution = jobLauncher.run(job, jobParameters);

            // ★ 결과 로그 출력
            log.info("Job ID: {}, Status: {}, ExitStatus: {}",
                    execution.getJobId(),
                    execution.getStatus(),
                    execution.getExitStatus());

            return "정산 배치 실행 완료 (ID: " + execution.getJobId() + ", 상태: " + execution.getStatus() + ")";
        } catch (Exception e) {
            log.error("배치 실행 실패", e);
            return "실패: " + e.getMessage();
        }
    }
}