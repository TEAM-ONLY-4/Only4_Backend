package com.ureca.only4_be.batch.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    // ★ 1. 하이버네이트 통계를 꺼내기 위한 EntityManager 주입
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/settlement")
    public String runSettlementJob(@RequestParam(value = "date", required = false) String date) {
        try {
            // ==========================================
            // [준비] 통계 초기화 & 스톱워치 시작
            // ==========================================

            // 1. 하이버네이트 통계 객체 가져오기 및 0으로 리셋
            Session session = entityManager.unwrap(Session.class);
            Statistics stats = session.getSessionFactory().getStatistics();
            stats.setStatisticsEnabled(true); // 강제로 통계 기능 ON
            stats.clear(); // 기존 누적 카운트 0으로 초기화

            // 2. 스톱워치 시작
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            String targetDate = (date != null) ? date : LocalDate.now().toString();
            log.info("[API Trigger] 정산 배치 실행 시작! (Target: {})", targetDate);

            // ==========================================
            // [실행] 배치 작업 수행
            // ==========================================
            Job job = jobRegistry.getJob("settlementJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", targetDate)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, jobParameters);

            // ==========================================
            // [종료] 측정 종료 및 결과 집계
            // ==========================================
            stopWatch.stop();
            long queryCount = stats.getPrepareStatementCount(); // 실행된 쿼리 개수 가져오기

            // 로그 출력
            log.info("Job: [settlementJob] completed in {}s", stopWatch.getTotalTimeSeconds());
            log.info("Query Count: {} queries executed", queryCount);
            log.info("Job ID: {}, Status: {}, ExitStatus: {}",
                    execution.getJobId(),
                    execution.getStatus(),
                    execution.getExitStatus());

            // ★ 브라우저 화면에 보여줄 최종 결과 메시지
            String resultMessage = String.format(
                    "<pre>\n" +
                            "✅ 정산 배치 실행 완료!\n" +
                            "----------------------------------\n" +
                            "- Job ID      : %d\n" +
                            "- 상태        : %s\n" +
                            "- 소요 시간   : %.3f초\n" +
                            "- 실행 쿼리수 : %d개 (Hibernate Statistics)\n" +
                            "</pre>",
                    execution.getJobId(),
                    execution.getStatus(),
                    stopWatch.getTotalTimeSeconds(),
                    queryCount
            );

            return resultMessage;

        } catch (Exception e) {
            log.error("배치 실행 실패", e);
            return "실패: " + e.getMessage();
        }
    }
}